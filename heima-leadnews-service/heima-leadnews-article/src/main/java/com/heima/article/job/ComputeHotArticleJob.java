package com.heima.article.job;


import com.heima.article.service.HotArticleService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ComputeHotArticleJob {
    @Autowired
    private HotArticleService hotArticleService;
    @XxlJob("computeHotArticleJob")
    public void computeHotArticleJob() {
        log.info("开始计算热门文章");
        hotArticleService.computeHotArticle();
        log.info("计算热门文章结束");
    }


}
