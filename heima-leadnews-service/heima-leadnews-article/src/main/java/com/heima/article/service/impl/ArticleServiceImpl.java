package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ApArticleConfigMapper;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ArticleMapper;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.article.service.ArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.common.constants.CacheService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.article.vo.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static com.heima.common.constants.ArticleConstants.MAX_PAGE_SIZE;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, ApArticle> implements ArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;
    @Autowired
    private ApArticleContentMapper apArticleContentMapper;
    @Autowired
  private   CacheService cacheService;
@Autowired
private ArticleFreemarkerService articleFreemarkerService;
    /**
     * 文章刷新
     *
     * @param dto
     * @param type 1 为加载更多 2加载最新
     * @return
     */

    @Override
    public ResponseResult loadArticleList(ArticleHomeDto dto, Short type) {

        //1.校验参数
        Integer size = dto.getSize();
        if (size == null || size == 0) {
            //如果传递过来的分页条数为0或者为null则默认查询10条数据
            size = 10;
        }
        //如果传递过来的数据过大,则使用定义的50条
        size = Math.min(size, MAX_PAGE_SIZE);
        dto.setSize(size);


        //如果参数既不是刷新文章也不是加载更多
        //给一个加载更多的默认值
        if (!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE) && !type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)) {
            //类型参数检验
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //文章频道校验
        if (StringUtils.isEmpty(dto.getTag())) {
            //如果参数没有传递则默认查询首页
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }

        //时间校验
        if (dto.getMaxBehotTime() == null) {
            //如果时间没有传递则默认从当前时间开始查
            dto.setMaxBehotTime(new Date());
        }
        if (dto.getMinBehotTime() == null) {

            dto.setMinBehotTime(new Date());
        }
        //2.查询数据
        List<ApArticle> apArticles = articleMapper.loadArticleList(dto, type);

        //3.结果封装
        return ResponseResult.okResult(apArticles);

    }

    /**
     *
     * @param loadtype  1 加载更多   2 加载最新
     * @param dto
     * @param firstPage  是否是首页
     * @return
     */
    @Override
    public ResponseResult load2(Short loadtype, ArticleHomeDto dto, boolean firstPage) {

        //是否为加载首页判断是首页的话话则加载得分最高的30篇文章 ,从redis中获取
        if(firstPage){

            //加载首页
            String all = cacheService.get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if (StringUtils.isNotBlank(all)){
                //转为对象
                List<HotArticleVo> hotArticleVos = JSON.parseArray(all, HotArticleVo.class);
                ResponseResult responseResult = ResponseResult.okResult(hotArticleVos);
                return responseResult;
            }
        }
        return loadArticleList(dto,loadtype);
    }

    /**
     * 保存app端文章
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional
    public ResponseResult saveArticle(ArticleDto dto) {
        /**
         * //1.检查参数:content == null 操作结束
         * //2.判断是否存在id
         * 	//2.1 不存在id，则保存文章、文章配置、文章内容
         * 	//2.2 存在id，则修改文章、文章内容
         * //3.结果返回  文章的id
         */
        //测试服务熔断降级
//        try {
//            Thread.sleep(11000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        //1.检查参数:content == null 操作结束
        if (dto.getContent() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //属性拷贝
        ApArticle apArticle = new ApArticle();
        apArticle.setPublishTime(dto.getPublishTime());
        BeanUtils.copyProperties(dto, apArticle);
        //2.判断是否存在id
        if (dto.getId() == null) {
            //2.1 不存在id，则保存文章、文章配置、文章内容
            this.save(apArticle);
            //保存文章的配置
            ApArticleConfig apArticleConfig = new ApArticleConfig(apArticle.getId());
            apArticleConfigMapper.insert(apArticleConfig);
            //保存文章的内容
            ApArticleContent apArticleContent = new ApArticleContent();
            //设置文章id
            apArticleContent.setArticleId(apArticle.getId());
            //设置文章内容
            apArticleContent.setContent(dto.getContent());
            //保存到文章配置表
            apArticleContentMapper.insert(apArticleContent);

        } else {
            //2.2 存在id，则修改文章、文章内容
            this.updateById(apArticle);
            //修改文章的内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);

        }
//        if (apArticle.getPublishTime() == null) {
//            apArticle.setPublishTime(new Date());
//        }
        articleFreemarkerService.buildArticleToMinIO(apArticle,dto.getContent());
        //3.结果返回  文章的id
        return ResponseResult.okResult(apArticle.getId());
    }

}
