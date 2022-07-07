package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserDto;

/**
 * @author 111
 */
public interface ApUserService  extends IService<ApUser> {

    ResponseResult login(ApUserDto dto);
}
