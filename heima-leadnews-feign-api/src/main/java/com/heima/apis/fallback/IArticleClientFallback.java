package com.heima.apis.fallback;

import com.heima.apis.article.IArticleClient;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * feign失败配置
 * @author itheima
 */
@Component
@Slf4j
public class IArticleClientFallback implements IArticleClient {
    @Override
    public ResponseResult saveArticle(ArticleDto dto) {
        log.error("远程调用文章微服务失败，服务降级！");
        //Redis，微信，短信 ——》 推送错误消息给运维人员，尽快检查服务器
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"获取数据失败");
    }
}