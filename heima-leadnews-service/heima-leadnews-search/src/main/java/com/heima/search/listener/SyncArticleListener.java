package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.search.vos.SearchArticleVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 接收消息
 */
@Slf4j
@Component
public class SyncArticleListener {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @KafkaListener(topics = ArticleConstants.ARTICLE_ES_SYNC_TOPIC)
    public void onMessage(String message){
        if (StringUtils.isNotBlank(message)){
            SearchArticleVo searchArticleVo = JSON.parseObject(message, SearchArticleVo.class);
            IndexRequest indexRequest = new IndexRequest();
            //设置索引id
            indexRequest.id(searchArticleVo.getId().toString());
            //告诉es发送的json格式
            indexRequest.source(message, XContentType.JSON);

            try {
                //发送请求
                restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("sync es error={}", e.getMessage());
            }


        }
    }
}
