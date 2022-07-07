package com.heima.article;


import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.json.async.NonBlockingJsonParser;
import com.heima.article.mapper.ApArticleContentMapper;
import com.heima.article.mapper.ArticleMapper;
import com.heima.article.service.ApArticleContentService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class ArticleTset {
    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;


    @Autowired
    private ArticleMapper apArticleMapper;

    @Autowired
    private ApArticleContentMapper contentService;

    @Test
    public void createStaticUrlTest() throws TemplateException, IOException {
        LambdaQueryWrapper<ApArticleContent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApArticleContent::getArticleId, 1302862387124125698L);
//获取文章内容
        ApArticleContent articleContent = contentService.selectOne(wrapper);
        //判断文章是否为空,文章内容是否为空
        if (articleContent != null && StringUtils.isNotBlank(articleContent.getContent())
        ) {
            //利用模板生成文章内容
            StringWriter writer = new StringWriter();
            //引用哪个模板
            Template template = configuration.getTemplate("article.ftl");
            Map<String, Object> map=new HashMap<>();
            //map存入数据,键需要跟模板中解析的遍历的值一样
            map.put("content", JSONArray.parseArray(articleContent.getContent()));
            //
            template.process(map,writer);
            InputStream is = new ByteArrayInputStream(writer.toString().getBytes());
            //上传到minio中
            String path = fileStorageService.uploadHtmlFile("", articleContent.getArticleId() + ".html", is);
            //修改文章表 把url写入进去

            ApArticle article=new ApArticle();
            article.setStaticUrl(path);
            article.setId(articleContent.getArticleId());
            article.setStaticUrl(path);
            apArticleMapper.updateById(article);


        }

    }


}
