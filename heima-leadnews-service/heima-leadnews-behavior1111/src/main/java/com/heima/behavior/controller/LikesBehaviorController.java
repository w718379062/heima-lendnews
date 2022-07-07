package com.heima.behavior.controller;

import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.model.common.dtos.ResponseResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 */
@RestController
@RequestMapping("/api/v1/likes_behavior")
@Slf4j
public class LikesBehaviorController {

    @Autowired
    private ApLikesBehaviorService apLikesBehaviorService;

    /**
     * 点赞和取消点赞
     * @param likesBehaviorDto 点赞参数
     * @return 点赞结果
     */
    @PostMapping()
    public ResponseResult praiseAndNoPraise(@RequestBody LikesBehaviorDto likesBehaviorDto){
        log.info("接收的参数是{}",likesBehaviorDto);

        return apLikesBehaviorService.praiseAndNoPraise(likesBehaviorDto);
    }
}
