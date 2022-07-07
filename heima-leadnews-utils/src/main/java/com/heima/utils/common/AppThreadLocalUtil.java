package com.heima.utils.common;

import com.heima.model.user.pojos.ApUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
public class AppThreadLocalUtil {

    private final static ThreadLocal<ApUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    //存入线程中
    public static void setUser(ApUser apUser){
        log.info("开始存放到线程中{}",apUser);

        WM_USER_THREAD_LOCAL.set(apUser);
    }

    //从线程中获取
    public static ApUser getUser(){
        log.info("从线程中取出数据{}",WM_USER_THREAD_LOCAL.get());
        return WM_USER_THREAD_LOCAL.get();
    }

    //清理
    public static void clear(){
        WM_USER_THREAD_LOCAL.remove();
    }

}