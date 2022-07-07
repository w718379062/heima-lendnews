package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface ArticleService extends IService<ApArticle> {
    ResponseResult loadArticleList (ArticleHomeDto dto, Short type);

    /**
     * 保存app端相关文章
     * @param dto
     * @return
     */
    ResponseResult saveArticle(ArticleDto dto) ;
    /**
     * 根据参数加载文章列表  v2
     * @param loadtype  1 加载更多   2 加载最新
     * @param dto
     * @param firstPage  是否是首页
     * @return
     */
    public ResponseResult load2(Short loadtype, ArticleHomeDto dto, boolean firstPage);




}
