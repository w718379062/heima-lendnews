package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ArticleFreemarkerService;
import com.heima.article.service.ArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.search.vos.SearchArticleVo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@Service
//@Async //开启异步调用
@Transactional
@Slf4j
public class ArticleFreemarkerServiceImpl implements ArticleFreemarkerService {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private Configuration configuration;
    @Autowired
    private ArticleService apArticleService;
    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;
    /**
     * 生成静态页面并上传到minio中
     * @param apArticle
     * @param content
     */

    /**
     * @param apArticle 文章
     * @param content   文章内容
     */
    @Async//开启异步调用
    @Override
    public void buildArticleToMinIO(ApArticle apArticle, String content) {
        //判断文章内容不为在继续生成静态文件
        String path=null;
        if (StringUtils.isNotBlank(content)) {
            //已知文章id
            Template template = null;
            StringWriter out = new StringWriter();
            try {
                //resources/templates/article.ftl
                template = configuration.getTemplate("article.ftl");
                //数据模型
                Map<String, Object> contentDataModel = new HashMap<>();
                contentDataModel.put("content", JSONArray.parseArray(content));
                //合成
                template.process(contentDataModel, out);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //4.3 把html文件上传到minio中
            InputStream in = new ByteArrayInputStream(out.toString().getBytes());
            path = fileStorageService.uploadHtmlFile("", apArticle.getId() + ".html", in);


            //4.4 修改ap_article表，保存static_url字段
            apArticleService.update(Wrappers.<ApArticle>lambdaUpdate().eq(ApArticle::getId, apArticle.getId())
                    .set(ApArticle::getStaticUrl, path));
        }
//发送消息，创建索引
        createArticleESIndex(apArticle, content, path);

    }
    private void createArticleESIndex(ApArticle apArticle, String content, String path) {
        SearchArticleVo vo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle,vo);
        vo.setContent(content);
        vo.setStaticUrl(path);
        kafkaTemplate.send(ArticleConstants.ARTICLE_ES_SYNC_TOPIC, JSON.toJSONString(vo));
    }

}

