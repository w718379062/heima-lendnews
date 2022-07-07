package com.heima.article.controller.v1;

import com.heima.article.service.ArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/article")
public class ArticleHomeController {

    @Autowired
    private ArticleService articleService;

    /**
     * 加载首页
     * @param dto
     * @return
     */
    @PostMapping("/load")
    public ResponseResult load (@RequestBody ArticleHomeDto dto){


      //  return articleService.loadArticleList(dto, ArticleConstants.LOADTYPE_LOAD_MORE);
        return articleService.load2(ArticleConstants.LOADTYPE_LOAD_MORE,dto,true);


    }

    /**
     * 下一页 ,加载更多
     * @param dto
     * @return
     */
    @PostMapping("/loadmore")
    public ResponseResult loadMore (@RequestBody ArticleHomeDto dto){

        return articleService.loadArticleList(dto, ArticleConstants.LOADTYPE_LOAD_MORE);


    }

    /**
     * 下拉刷新
     * @param dto
     * @return
     */
    @PostMapping("/loadnew")
    public ResponseResult loadNew (@RequestBody ArticleHomeDto dto){

        return articleService.loadArticleList(dto, ArticleConstants.LOADTYPE_LOAD_NEW);


    }



}
