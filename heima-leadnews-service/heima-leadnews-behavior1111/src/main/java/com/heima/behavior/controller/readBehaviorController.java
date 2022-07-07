package com.heima.behavior.controller;

import com.baomidou.mybatisplus.extension.api.R;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/read_behavior")
@Slf4j
public class readBehaviorController {

    @Autowired
    private ApLikesBehaviorService apLikesBehaviorService;

    /**
     * 文章的阅读次数
     * @param dto
     * @return
     */
    @PostMapping
    public ResponseResult likesBehavior(@RequestBody LikesBehaviorDto dto){
        log.info("接收到的参数是{}",dto);
        return apLikesBehaviorService.likesBehavior(dto);
    }

}
