package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.heima.apis.article.IArticleClient;
import com.heima.common.aliyun.GreenImageScan;
import com.heima.common.aliyun.GreenTextScan;
import com.heima.common.exception.CustomException;
import com.heima.common.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.mapper.WmChannelMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmSensitiveMapper;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmNewsAutoScanService;
import com.heima.wemedia.service.WmSensitiveService;
import javassist.expr.NewArray;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class WmNewsAutoScanServiceImpl implements WmNewsAutoScanService {


    @Autowired
    private WmNewsMapper wmNewsMapper;
    @Autowired
    private GreenTextScan greenTextScan;
    @Autowired
    private GreenImageScan greenImageScan;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private IArticleClient articleClient;
    @Autowired
    private WmChannelMapper wmChannelMapper;
    @Autowired
    private WmUserMapper wmUserMapper;

    /**
     * 自媒体文章审核
     *
     * @param wmNewsId 自媒体文章id
     *                 Async 表明这是一个异步方法
     */
    @Async //表示是异步调用
    @Override
    public void autoScanWmNews(Integer wmNewsId) {
        /**
         * 1.查询自媒体文章
         *  2.审核文本内容  阿里云接口
         *  3.审核图片  阿里云接口
         *  4.审核成功，远程调用文章微服务保存app端用户相关的文章数据
         */
        //查询自媒体文章
        WmNews wmNews = wmNewsMapper.selectById(wmNewsId);
        if (wmNews == null) {
            log.info("查询出的文章为空");
            //抛出异常
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //判断当前状态是否在待审核状态,是的话继续下面的步骤
        if (wmNews.getStatus().equals(WmNews.Status.SUBMIT.getCode())) {
            Map<String, Object> textAndImages = handleTextAndImages(wmNews);
            //自定义敏感词审核
          boolean handleSenBoolean=  handleSensitiveScan((String) textAndImages.get("text"), wmNews);
          if (!handleSenBoolean){
              return;
          }
            //审核有可能成功也有可能失败,需要修改文章的状态
            boolean textBoolean = reviewArticles((String) textAndImages.get("text"), wmNews);
            //取反结束方法
            if (!textBoolean) {
                log.error("审核失败请查看详细信息");
                return;
            }
            boolean imagesBoolean = reviewImages((List<String>) textAndImages.get("images"), wmNews);
            if (!imagesBoolean) {
                return;
            }

//4.审核成功，远程调用文章微服务保存app端用户相关的文章数据

            ResponseResult responseResult = saveAppArticle(wmNews);
            if (!responseResult.getCode().equals(AppHttpCodeEnum.SUCCESS.getCode())) {
                //远程调用没有保存成功
                System.out.println(responseResult.getErrorMessage());
                throw new RuntimeException(
                        "WmNewsAutoScanServiceImpl-文章审核，保存app端相关文章数据失败");
            }
            log.info("调用文章更新方法");
            //回填article_id
            wmNews.setArticleId((Long) responseResult.getData());
            wmNews.setStatus(WmNews.Status.PUBLISHED.getCode());
            wmNews.setReason("审核成功");
            wmNewsMapper.updateById(wmNews);
        }
    }

    @Autowired
    private WmSensitiveMapper wmSensitiveService;

    /**
     * 自定义敏感词的审核
     * @param text 文本内容
     * @param wmNews 文章表实体类
     * @return 返回Boolean
     */
    private boolean handleSensitiveScan(String text, WmNews wmNews) {
        boolean flag=true;
        //获取所有敏感词
        LambdaQueryWrapper<WmSensitive>wrapper=new LambdaQueryWrapper<>();
        //查询出所有敏感词
        wrapper.select(WmSensitive::getSensitives);
        List<WmSensitive> wmSensitives = wmSensitiveService.selectList(wrapper);
        /*遍历出查询到的敏感词*/
        List<String> sensitiveList = wmSensitives.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());
        //初始化敏感词库
        SensitiveWordUtil.initMap(sensitiveList);
        //查看文章中是否包含敏感词
        Map<String, Integer> matchWords = SensitiveWordUtil.matchWords(text);
        //判断查询出来的map集合是否为空,为空则代表没有违禁词
        if (matchWords.size()>0){
            //如果map集合的长度大于0代表文本中有违禁词
            flag=false;
            //审核失败理由
            wmNews.setReason("文章中有违禁词");
            //更改状态为审核失败
            wmNews.setStatus(WmNews.Status.FAIL.getCode());
            wmNewsMapper.updateById(wmNews);

        }


        return flag;

    }

    private ResponseResult saveAppArticle(WmNews wmNews) {

        ArticleDto dto = new ArticleDto();
        //属性的拷贝
        BeanUtils.copyProperties(wmNews, dto);
        //文章的布局
        dto.setLayout(wmNews.getType());
        //频道
        WmChannel wmChannel = wmChannelMapper.selectById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelName(wmChannel.getName());
        }

        //作者
        dto.setAuthorId(wmNews.getUserId().longValue());
        WmUser wmUser = wmUserMapper.selectById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorName(wmUser.getName());
        }
        //设置文章id
        if (wmNews.getArticleId() != null) {
            dto.setId(wmNews.getArticleId());
        }
        dto.setCreatedTime(new Date());

        ResponseResult responseResult = articleClient.saveArticle(dto);
        return responseResult;
    }

    @Autowired
    private Tess4jClient tess4jClient;
    /**
     * 审核图片,以及封面内容
     *
     * @param images
     * @param wmNews
     * @return
     */
    private boolean reviewImages(List<String> images, WmNews wmNews) {

        boolean flag = true;
        List<byte[]> listImages = null;
        try {
            //参数的校验
            if (images == null || images.size() == 0) {
                return flag;
            }
            //图片去重
            images = images.stream().distinct().collect(Collectors.toList());
            listImages = new ArrayList<>();
            for (String image : images) {
                //下载图片 minIO
                byte[] bytes = fileStorageService.downLoadFile(image);
                ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                BufferedImage imageFile = ImageIO.read(in);
                //识别图片的文字
                String result = tess4jClient.doOCR(imageFile);

                //审核是否包含自管理的敏感词
                boolean isSensitive = handleSensitiveScan(result, wmNews);
                if(!isSensitive){
                    return isSensitive;
                }
                listImages.add(bytes);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //审核图片
        try {
            Map map = greenImageScan.imageScan(listImages);

            //审核失败

            if (map.get("suggestion").equals("block")) {
                flag = false;
                wmNews.setReason("图片违规");
                wmNews.setStatus(WmNews.Status.FAIL.getCode());
                wmNewsMapper.updateById(wmNews);
            }
            //图片存在不确定内容
            if (map.get("suggestion").equals("review")) {
                flag = false;
                wmNews.setReason("图片存在不确定内容,需要人工进一步审核");
                wmNews.setStatus(WmNews.Status.ADMIN_AUTH.getCode());
                wmNewsMapper.updateById(wmNews);
            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }

        return flag;

    }

    /**
     * 文章的自动审核
     *
     * @param text
     * @param wmNews
     * @return
     */
    private boolean reviewArticles(String text, WmNews wmNews) {
        //默认文章为审核通过
        boolean flag = true;

        //调用阿里云审核文本内容的方
        try {
            Map map = greenTextScan.greeTextScan(text);
            log.info("当先审核结果为:{}",map.get("suggestion").toString());
            //判断map是否为空有可能阿里宕机
            if (map != null) {
                //不为空则判断审核情况
                //需要人工审核
                if (map.get("suggestion").equals("review")) {
                    log.info("当先审核结果为:{}",map.get("suggestion").toString());
                    flag = false;
                    wmNews.setStatus(WmNews.Status.ADMIN_AUTH.getCode());
                    wmNews.setReason("文章内容不确定需要人工审核");
                    wmNewsMapper.updateById(wmNews);
                }
                //文本违规
                if (map.get("suggestion").equals("block")) {
                    log.info("当先审核结果为:{}",map.get("suggestion").toString());
                    flag = false;
                    //如果文本违规则更改状态审核失败
                    wmNews.setStatus(WmNews.Status.FAIL.getCode());
                    wmNews.setReason("文章内容违规");
                    wmNewsMapper.updateById(wmNews);
                }


            }
        } catch (Exception e) {
            flag = false;
            e.printStackTrace();
        }
        log.info("文本没有违规"+flag);

        return flag;
    }

    /**
     * * 1.从自媒体文章的内容中提取文本和图片
     * * 2.提取文章的封面图片
     *
     * @param wmNews
     * @return
     */
    private Map<String, Object> handleTextAndImages(WmNews wmNews) {

        //存储纯文本内容,文章的标题也需要审核,所以也需要添加进去
        StringBuilder articleText = new StringBuilder(wmNews.getTitle());
        //存储封面图片和内容图片
        List<String> images = new ArrayList<>();

        //校验参数
        if (StringUtils.isNotBlank(wmNews.getContent())) {
            //将文章内容解析为map结构的内容
            List<Map> maps = JSON.parseArray(wmNews.getContent(), Map.class);
            //遍历map
            for (Map map : maps) {
                //判断是否为文章
                if (map.get("type").equals("text")) {
                    //保存为字符串
                    articleText.append(map.get("value"));
                }
                //提取出内容图片
                if (map.get("type").equals("image")) {
                    //将图片信息保存进list集合中
                    images.add((String) map.get("value"));
                }
            }
        }
        // 2.提取文章的封面图片
        if (StringUtils.isNotBlank(wmNews.getImages())) {
            //封面图片可能是多个,需要使用逗号分割
            String[] split = wmNews.getImages().split(",");
            images.addAll(Arrays.asList(split));
        }
        Map<String, Object> map = new HashMap<>();
        //将文章的内容放入map集合中
        map.put("text", articleText.toString());
        //将图片信息放入到集合中
        map.put("images", images);
        return map;

    }

}
