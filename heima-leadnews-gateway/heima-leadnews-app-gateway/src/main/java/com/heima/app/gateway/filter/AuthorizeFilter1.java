package com.heima.app.gateway.filter;

import com.heima.app.gateway.utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.checkerframework.checker.index.qual.SameLen;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Slf4j
public class AuthorizeFilter1 implements Ordered, GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取request
        ServerHttpRequest request = exchange.getRequest();
        //获取response
        ServerHttpResponse response = exchange.getResponse();
        URI uri = request.getURI();

        if (uri.getPath().contains("/login")) {


            log.info("登录拦截到了准备放行");
            //如果包含则代表用户是来进行登录的,直接放行
            return chain.filter(exchange);
        }
        if (uri.getPath().contains("/article")) {


            log.info("查询文章的请求进来了准备放行路径是------->{}", uri);
            //如果包含则代表用户是来进行登录的,直接放行
            return chain.filter(exchange);
        }
        try {
            //判断token是否存在
            String token = request.getHeaders().getFirst("token");
            if (StringUtils.isBlank(token)) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
            //判断token是否在有效期内
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int verifyToken = AppJwtUtil.verifyToken(claimsBody);
            if (verifyToken == 1 || verifyToken == 2) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
//获取用户信息
            Object userId = claimsBody.get("id");
            System.out.println("获取到的user" + userId.toString());
//存储header中
            ServerHttpRequest serverHttpRequest = request.mutate().header("userId", userId + "").build();
            System.out.println("发送数据" + serverHttpRequest.getHeaders());
//重置请求
            exchange.mutate().request(serverHttpRequest);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("无效的token");

            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        return chain.filter(exchange);

    }

    /**
     * 优先级设置
     * 数字越小优先级越高
     *
     * @return
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
