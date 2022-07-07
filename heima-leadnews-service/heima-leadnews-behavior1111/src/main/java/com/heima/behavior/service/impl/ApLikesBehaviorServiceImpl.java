package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.common.constants.CacheService;
import com.heima.common.constants.LikesBehaviorConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.WmThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.LICENSE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.applet.AppletThreadGroup;

@Slf4j
@Service
@Transactional
public class ApLikesBehaviorServiceImpl implements ApLikesBehaviorService {
    @Autowired
    private CacheService cacheService;

    /**
     * 用户行为1点赞和取消点赞
     * 点赞
     *
     * @param likesBehaviorDto 数据 0 点赞   1 取消点赞
     * @return
     */
    @Override
    public ResponseResult praiseAndNoPraise(LikesBehaviorDto likesBehaviorDto) {
        //校验参数
        if (likesBehaviorDto == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
//        //判断用户是否登录
//        WmUser user = WmThreadLocalUtils.getUser();
        //用户的行为
        UpdateArticleMess articleMess = new UpdateArticleMess();
        //设置文章id
        articleMess.setArticleId(likesBehaviorDto.getArticleId());
        //记录用户行为
        articleMess.setType(UpdateArticleMess.UpdateArticleType.LIKES);
//3、设计hash存储结构：LIKE_behavior_articeltId userId {articleId: 234232, type:0, operation: 1}
        //记录用户点赞

        //判断用户是否是点赞的行为
        if (likesBehaviorDto.getOperation() == 0) {
            //存入到redis中
            //获取这个key

            Object obj = cacheService.hGet(LikesBehaviorConstants.LIKE_BEHAVIO + likesBehaviorDto.getArticleId(), "5000");
            //判断这个key是否存在,存在则提示已点赞
            if (obj != null) {
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "已点赞");
                //创建这个key
//                log.info("保存当前key:{} ,{}, {}", dto.getArticleId(), user.getId(), dto);
//                cacheService.hPut(BehaviorConstants.LIKE_BEHAVIOR + dto.getArticleId().toString(), user.getId().toString(), JSON.toJSONString(dto));
//                mess.setAdd(1);
            }else {

                log.info("保存当前key:{} ,{}, {}", likesBehaviorDto.getArticleId(), "6666", likesBehaviorDto);
                //保存当前有用户对文章的点赞
                //保存当前key
                cacheService.hPut(LikesBehaviorConstants.LIKE_BEHAVIO + likesBehaviorDto.getArticleId(),"5000",JSON.toJSONString(likesBehaviorDto));
            }
        }else {
            //取消点赞
            cacheService.hDelete(LikesBehaviorConstants.LIKE_BEHAVIO + likesBehaviorDto.getArticleId(), "5000");

        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 自媒体的文章阅读次数
     * @param dto
     * @return
     */
    @Override
    public ResponseResult likesBehavior(LikesBehaviorDto dto) {
    //校验参数
        if(dto==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //取出阅读次数
        Integer count = dto.getCount();
        //判断redis中是否存在
        String renumber = cacheService.get("readNumber_"+dto.getArticleId());
        if (renumber==null){
            //不存在则创建这个key
            cacheService.set("readNumber_"+dto.getArticleId(),JSON.toJSONString(dto));
        }else {
            //存在则更新redis中数据
            //取出原有的数据更新最新的数据
            LikesBehaviorDto likesBehaviorDto = JSON.parseObject(renumber, LikesBehaviorDto.class);
            //将阅读次数更新为最新的数据
            likesBehaviorDto.setCount(likesBehaviorDto.getCount()+count);
            cacheService.set("readNumber_"+dto.getArticleId(),JSON.toJSONString(likesBehaviorDto));
        }
        //kuu
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
