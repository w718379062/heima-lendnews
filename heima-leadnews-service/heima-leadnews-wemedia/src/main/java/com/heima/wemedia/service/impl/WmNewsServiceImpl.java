package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WemediaConstants;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.common.exception.CustomException;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.WmThreadLocalUtils;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmNewsService;
import com.heima.wemedia.service.WmNewsTaskService;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.C;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {
    @Autowired
    private WmNewsAutoScanService wmNewsAutoScanService;

    /**
     * 文章查询
     *
     * @param dto
     * @return
     */

    @Override
    public ResponseResult list(WmNewsPageReqDto dto) {
        //校验参数
        if (dto == null) {
            //如果参数为空则返回无效的参数
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //分页参数检查
        dto.checkParam();
        // LambdaQueryWrapper<WmNews> queryWrapper= new LambdaQueryWrapper();
        //获取当前登录人的信息
        WmUser user = WmThreadLocalUtils.getUser();

        //2.分页条件查询
        Page page = new Page(dto.getPage(), dto.getSize());
        LambdaQueryWrapper<WmNews> wrapper = new LambdaQueryWrapper<>();
        //状态精确查询
        wrapper.eq(dto.getStatus() != null, WmNews::getStatus, dto.getStatus());
        //频道精确查询
        wrapper.eq(dto.getChannelId() != null, WmNews::getChannelId, dto.getChannelId());
        //时间范围查询
        wrapper.between(dto.getBeginPubDate() != null && dto.getEndPubDate() != null, WmNews::getPublishTime, dto.getBeginPubDate(), dto.getEndPubDate());
        //关键字模糊查询
        wrapper.like(dto.getKeyword() != null, WmNews::getTitle, dto.getKeyword());
        //查询当前登录用户的文章
        if (user.getId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        wrapper.eq(WmNews::getUserId, user.getId());

        //发布时间倒序查询
        wrapper.orderByDesc(WmNews::getCreatedTime);
        this.page(page, wrapper);
        //3.结果返回
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int) page.getTotal());
        responseResult.setData(page.getRecords());


        return responseResult;
    }

    /**
     * 发布文章,修改文章,保存为草稿
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional
    public ResponseResult submitNews(WmNewsDto dto) {

        //参数校验
        if (dto == null) {
            //如果参数为空则返回无效参数
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //1.保存或修改文章 -》 wm_news
        WmNews wmNews = new WmNews();
        //属性拷贝,属性名一致,参数类型一致才回进行拷贝
        BeanUtils.copyProperties(dto, wmNews);
        //封面图片的设置
        if (dto.getImages() != null && dto.getImages().size() != 0) {
            //
            StringUtils.join(dto.getImages(), ",");
        }
        //封面的属性
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {
            //如果封面的属性为-1则先设置为null
            wmNews.setType(null);
        }
        saveOrUpdateWmNews(wmNews);
        //2.判断是否为草稿(status == 0)  如果为草稿结束当前方法: return success
        if (dto.getStatus().equals(WmNews.Status.NORMAL.getCode())) {
            //如果为草稿返回操作成功
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
        }

        //3.不是草稿，保存文章内容图片与素材的关系 -> wm_news_material

        ArrayList<String> images = getStrings(dto.getContent());
        //3.2保存内容图片和素材的关系
        log.info(images.toString());
        saveRelativeInfo(images, wmNews.getId(), dto.getType());
        //4.不是草稿，保存文章封面图片与素材的关系，如果当前布局是自动，需要匹配封面图片->wm_news_material
        saveRelativeInfoForCover(dto, wmNews, images);
        //自动审核文章
        //wmNewsAutoScanService.autoScanWmNews(wmNews.getId());
        //延迟消费任务
        wmNewsTaskService.addNewsToTask(wmNews);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 文章的上下架操作
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {

        //检查参数
        if (dto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询文章
        WmNews wmNews = this.getById(dto.getId());
        if (wmNews == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        //判断文章是否已发布
        if (!wmNews.getStatus().equals(WmNews.Status.PUBLISHED.getCode())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "当前文章不是发布状态，不能上下架");
        }
        //4.修改文章enable
        if (dto.getEnable() != null && dto.getEnable() > -1 && dto.getEnable() < 2) {
            update(Wrappers.<WmNews>lambdaUpdate().set(WmNews::getEnable, dto.getEnable())
                    .eq(WmNews::getId, wmNews.getId()));
        }
//发送消息，通知article端修改文章配置
        if (wmNews.getArticleId() != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("articleId", wmNews.getArticleId());
            map.put("enable", dto.getEnable());
            kafkaTemplate.send(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC, JSON.toJSONString(map));
        }
        //偷懒实现方式
        //boolean result = this.lambdaUpdate().set(WmNews::getEnable, dto.getEnable())
        //        .eq(WmNews::getId, dto.getId())
        //        .eq(WmNews::getStatus, WmNews.Status.PUBLISHED.getCode())
        //        .update();
        //if (result) { //更新成功
        //    //发消息
        //} else { //更新失败
        //    return ResponseResult.errorResult(AppHttpCodeEnum.UNKNOWN, "非法操作");
        //}
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    /**
     * 第一个功能：如果当前封面类型为自动，则设置封面类型的数据
     * 匹配规则：
     * 1，如果内容图片大于等于1，小于3  单图  type 1
     * 2，如果内容图片大于等于3  多图  type 3
     * 3，如果内容没有图片，无图  type 0
     * <p>
     * 第二个功能：保存封面图片与素材的关系
     *
     * @param dto
     * @param wmNews 文章实体类
     * @param
     */
    private void saveRelativeInfoForCover(WmNewsDto dto, WmNews wmNews, ArrayList<String> images) {
        //接收前端传递过来的素材url
        List<String> dtoImages = dto.getImages();
        //如果当前封面类型为自动,则设置封面类型的数据
        if (dto.getType().equals(WemediaConstants.WM_NEWS_TYPE_AUTO)) {

//          * 1，如果内容图片大于等于1，小于3  单图  type 1
            if (dtoImages.size() >= 1 && dtoImages.size() < 3) {
                wmNews.setType(WemediaConstants.WM_NEWS_SINGLE_IMAGE);

                dtoImages = images.stream().limit(1).collect(Collectors.toList());
            } else if (dtoImages.size() >= 3) {
//                 2，如果内容图片大于等于3  多图  type 3
                //多图
                wmNews.setType(WemediaConstants.WM_NEWS_MANY_IMAGE);
                dtoImages = images.stream().limit(1).collect(Collectors.toList());
            } else {
                //无图
//                * 3，如果内容没有图片，无图  type 0
                wmNews.setType(WemediaConstants.WM_NEWS_NONE_IMAGE);
            }
            //修改文章
            if (dtoImages != null && dtoImages.size() > 0) {
                saveRelativeInfo(dtoImages, wmNews.getId(), WemediaConstants.WM_COVER_REFERENCE);
            }
        }


    }


    /**
     * 读取内容中的图片信息
     *
     * @param content
     * @return
     */
    private ArrayList<String> getStrings(String content) {
        //拉取文章图片

        //3.1从文章内容中抽取图片
        ArrayList<String> images = new ArrayList<>();
        //JSONArray.parseArray把json格式的数据转换为集合,第一个参数为json格式的字符串,第二个为集合的类型
        List<Map> maps = JSONArray.parseArray(content, Map.class);
        for (Map map : maps) {
            if ("image".equals(map.get("type"))) {
                //取出文章中的图片地址
                String imageUrl = (String) map.get("value");
                //放入到集合中
                images.add(imageUrl);
            }
        }
        return images;
    }

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    private void saveOrUpdateWmNews(WmNews wmNews) {

        //补全属性
        //补全当前用户id
        wmNews.setUserId(WmThreadLocalUtils.getUser().getId());
        //补全创建时间
        wmNews.setCreatedTime(new Date());
        //发布时间
        //  wmNews.setPublishTime(new Date());
        //上架时间
        wmNews.setSubmitedTime(new Date());
        //设置默认为上架状态
        wmNews.setEnable((short) 1);
        //判断是否携带id
        if (wmNews.getId() != null) {
            //如果携带id代表是修改的操作
            //删除素材和文章关系表
            LambdaQueryWrapper<WmNewsMaterial> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WmNewsMaterial::getNewsId, wmNews.getId());
            wmNewsMaterialMapper.delete(wrapper);
            //更新WmNews表
            this.updateById(wmNews);
        } else {
            //如果没有携带id代表的是新增操作
            this.save(wmNews);
        }


    }

    /**
     * 保存文章与素材的关系
     *
     * @param materials
     * @param newsId
     * @param type
     */
    private void saveRelativeInfo(List<String> materials, Integer newsId, Short type) {

        //校验参数
        if (CollectionUtils.isEmpty(materials)) {
            return;
        }
        //根据图片信息查询素材的id
        LambdaQueryWrapper<WmMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WmMaterial::getUrl, materials);
        List<WmMaterial> wmMaterials = wmMaterialMapper.selectList(wrapper);

        //判断素材会否有效
        if (CollectionUtils.isEmpty(wmMaterials) || wmMaterials.size() != materials.size()) {
            //手动抛异常的第一个功能是提示调用者素材是失效了
            //第二个作用是:能用进行数据的回滚操作
            throw new CustomException(AppHttpCodeEnum.ATERIASL_REFERENCE_FAIL);
        }
        List<Integer> collect = wmMaterials.stream().map(WmMaterial::getId).collect(Collectors.toList());
        wmNewsMaterialMapper.saveRelations(collect, newsId, type);


    }


}
