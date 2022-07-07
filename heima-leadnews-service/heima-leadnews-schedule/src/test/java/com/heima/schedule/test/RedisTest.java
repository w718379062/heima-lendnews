package com.heima.schedule.test;

import com.heima.common.constants.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

@SpringBootTest
public class RedisTest {
    @Autowired
    private CacheService cacheService;

    @Test
    public void testList() throws InterruptedException {
        //在list的左边添加元素
        cacheService.lLeftPush("list_001","hello,redis1");
        //cacheService.lLeftPush("list_001","hello,redis2");
        //cacheService.lLeftPush("list_001","hello,redis3");

        //在list的右边获取元素，并删除

        Thread.sleep(5000);
        String list_001 = cacheService.lRightPop("list_001");
        System.out.println(list_001);
    }

    @Test
    public void testZset(){
        //添加数据到zset中  分值
        cacheService.zAdd("zset_key_001","hello zset 001",1000);
        cacheService.zAdd("zset_key_001","hello zset 002",8888);
        cacheService.zAdd("zset_key_001","hello zset 003",7777);
        cacheService.zAdd("zset_key_001","hello zset 005",7778);
        cacheService.zAdd("zset_key_001","hello zset 004",999999);

        //按照分值获取数据：zrange zset_key_001 0 8888 withscores
        //第一个参数为key,第二个参数为最小值,第三个参数为最大值
        //上面五个参数输出的结果为:[hello zset 001, hello zset 003, hello zset 005, hello zset 002]
        Set<String> zset_key_001 = cacheService.zRangeByScore("zset_key_001", 0, 8888);
        System.out.println(zset_key_001);
    }
}
