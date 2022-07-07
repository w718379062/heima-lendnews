package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserDto;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 11111
 */
@Service
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser>implements ApUserService {


    /**
     * 用户登录验证的方法
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(ApUserDto dto) {

        //判断传递数据是否为空用户登录和未登录逻辑处理
        if (StringUtils.isNotBlank(dto.getPassword())&&StringUtils.isNotBlank(dto.getPassword())){
            //如果不是空则代表的是登录用户
            LambdaQueryWrapper<ApUser> wrapper=new LambdaQueryWrapper<>();
            //根据手机号进行查询数据
            wrapper.eq(ApUser::getPhone,dto.getPhone());
            ApUser apUser = this.getOne(wrapper);
            //判断是是否查询出数据
            if (apUser==null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"查询不到本用户信息");

            }
            //如果查询到了数据进行密码比对
            String password = dto.getPassword();
            String salt = apUser.getSalt();
            //进行加盐md5加密
            //计算数据库存储密码=MD5(用户密码+盐)
            String pswd = DigestUtils.md5DigestAsHex((password + salt).getBytes());
            if (!pswd.equals(apUser.getPassword())){
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }
            //用户token
            String token = AppJwtUtil.getToken(apUser.getId().longValue());
            //返回token user
            Map<String,Object> map=new HashMap<>();
            //把token存入map中
            map.put("token",token);
            //把查询到的数据存入进去
            //把密码设置为空
            apUser.setPassword("");
            //把加盐秘钥设置为null
            apUser.setSalt(null);
            map.put("user",apUser);
            return ResponseResult.okResult(map);


        }else {

            //如果是游客进行登录的则生成id为0的token
            String token = AppJwtUtil.getToken(0L);
            Map<String,Object> map=new HashMap<>();
            map.put("token",token);
            return ResponseResult.okResult(map);
        }
    }
}
