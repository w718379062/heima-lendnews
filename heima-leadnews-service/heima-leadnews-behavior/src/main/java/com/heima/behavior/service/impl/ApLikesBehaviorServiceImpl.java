package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.common.constants.CacheService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.AppThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
@Slf4j
public class ApLikesBehaviorServiceImpl implements ApLikesBehaviorService {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Override
    public ResponseResult like(LikesBehaviorDto dto) {
        //1.检查参数
        if (dto == null || dto.getArticleId() == null || checkParam(dto)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.是否登录
        ApUser user = AppThreadLocalUtil.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //构建消息发送对象
        UpdateArticleMess mess = new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.LIKES);

        //3.点赞  保存数据
        if(dto.getOperation() == 0){
            //hash数据结构
            Object obj = cacheService.hGet(
                    "LIKE-BEHAVIOR-" + dto.getArticleId().toString(), user.getId().toString());
            if(obj != null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"已点赞");
            }
            // 保存当前key
            log.info("保存当前key:{} ,{}, {}", dto.getArticleId(), user.getId(), dto);
            cacheService.hPut(
                    "LIKE-BEHAVIOR-" + dto.getArticleId().toString(),
                    user.getId().toString(), //hashKey
                    JSON.toJSONString(dto)); //具体行为

            //点赞，分值+1
            mess.setAdd(1);
        }else { //取消点赞
            // 删除当前key
            log.info("删除当前key:{}, {}", dto.getArticleId(), user.getId());
            cacheService.hDelete("LIKE-BEHAVIOR-" + dto.getArticleId().toString(), user.getId().toString());

            //取消点赞，分值-1
            mess.setAdd(-1);
        }

        //发送消息，数据聚合
        kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC,JSON.toJSONString(mess));
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 检查参数
     * @return
     */
    private boolean checkParam(LikesBehaviorDto dto){
        if(dto.getType() > 2 || dto.getType() < 0 || dto.getOperation() > 1 || dto.getOperation() < 0){
            return true;
        }
        return false;
    }
}
