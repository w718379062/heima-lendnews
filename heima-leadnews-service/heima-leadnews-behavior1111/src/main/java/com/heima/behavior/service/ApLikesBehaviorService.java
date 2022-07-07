package com.heima.behavior.service;

import com.heima.model.common.dtos.ResponseResult;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApLikesBehaviorService{
    /**
     * 点赞和取消点赞
     * @param likesBehaviorDto 点赞参数
     * @return 点赞结果
     */
    public ResponseResult praiseAndNoPraise(LikesBehaviorDto likesBehaviorDto);

    /**
     * 文章的阅读次数
     * @param dto 阅读参数
     * @return 阅读结果
     */
    public ResponseResult likesBehavior( LikesBehaviorDto dto);
}
