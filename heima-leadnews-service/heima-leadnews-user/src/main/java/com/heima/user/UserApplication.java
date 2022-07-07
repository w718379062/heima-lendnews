package com.heima.user;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * user启动类
 * exclude排除哪些类,不进行加载
 */
@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@EnableDiscoveryClient//注册中心,不管用的什么注册中心都可以识别使用
@MapperScan("com.heima.user.mapper")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class,args);
    }
}
