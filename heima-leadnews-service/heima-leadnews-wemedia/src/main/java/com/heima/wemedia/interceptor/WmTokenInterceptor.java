package com.heima.wemedia.interceptor;

import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.WmThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
public class WmTokenInterceptor implements HandlerInterceptor {
    /**
     * 前置拦截器
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //取出用户Id
        String userId = request.getHeader("userId");
        if (userId!=null ){
            //如果不为空则存入到当前线程中
            WmUser wmUser=new WmUser();
            //将String类型转换为包装Integer类型
            wmUser.setId(Integer.valueOf(userId));
            //将数据保存到当前线程中,每个请求都是一个独立的线程,没有线程安全问题,数据也不会丢失
            WmThreadLocalUtils.setUser(wmUser);
            log.info("将用户数据保存到threadLocal 中成功 当前保存的用户id是{}",wmUser.getId());

        }
        return true;
    }

    /**
     *
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清理线程中的数据
        WmThreadLocalUtils.removeUser();
    }
}
