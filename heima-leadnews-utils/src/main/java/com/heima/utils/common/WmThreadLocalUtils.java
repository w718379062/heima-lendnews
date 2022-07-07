package com.heima.utils.common;

import com.heima.model.wemedia.pojos.WmUser;

public class WmThreadLocalUtils {
    private final static ThreadLocal<WmUser> WM_USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 添加用户
     * @param wmUser
     */
    public static void setUser(WmUser wmUser){
        WM_USER_THREAD_LOCAL.set(wmUser);
    }

    /**
     * 获取用户
     * @return
     */
    public static WmUser getUser(){
        return WM_USER_THREAD_LOCAL.get();
    }

    /**
     * 删除方法
     */
    public static void removeUser(){

        WM_USER_THREAD_LOCAL.remove();
    }
}
