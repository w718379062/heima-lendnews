package com.heima.search.service.Impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mongo.pojos.ApUserSearch;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.common.AppThreadLocalUtil;
import com.heima.search.service.ApUserSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ApUserSearchServiceImpl implements ApUserSearchService {

    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 用户搜索记录保存
     * @param keyword 搜索关键词
     * @param userId 用户id
     * @return
     */
    @Override
    @Async
    public void insert(String keyword, Integer userId) {
        //查询搜索词是否在数据库中存在
        Query query = Query.query(Criteria.where("userId").is(userId).and("keyword").is(keyword));
        ApUserSearch apUserSearch = mongoTemplate.findOne(query, ApUserSearch.class);
        //存在在数据库中更新操作时间
        if (apUserSearch!=null){
            apUserSearch.setCreatedTime(new Date());
            mongoTemplate.save(apUserSearch);
        }
        //不存在则新增到数据库中   判断搜索数是否大于10
        apUserSearch = new ApUserSearch();

        apUserSearch.setUserId(userId);
        apUserSearch.setCreatedTime(new Date());
        apUserSearch.setKeyword(keyword);
        Query userId1 = Query.query(Criteria.where("userId").is(userId));
        userId1.with(Sort.by(Sort.Direction.DESC,"createdTime"));
        List<ApUserSearch> apUserSearches = mongoTemplate.find(userId1, ApUserSearch.class);
        if (apUserSearches==null||apUserSearches.size()<10){
            mongoTemplate.save(apUserSearch); //少于10条，直接新增
        }else {
            //删除最早的搜索记录
            //获取到最早的搜索记录
            ApUserSearch apUserSearch1 = apUserSearches.get(apUserSearches.size() - 1);
            //同步最新的搜索记录,到数据库中
            mongoTemplate.findAndReplace(Query.query(Criteria.where("id").is(apUserSearch1.getId())),apUserSearch);


        }
    }

    /**
     *
     * 查询搜索历史记录
     * @return
     */
    @Override
    public ResponseResult findUserSearch() {
        //获取当前用户
        ApUser user = AppThreadLocalUtil.getUser();
        //游客登录也不加载
        if(user == null || user.getId() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }
        //根据用户查询数据，按照时间倒序
        List<ApUserSearch> apUserSearches =
                // where userId = ? order by createdTime desc;
                mongoTemplate.find(Query.query(Criteria.where("userId").is(user.getId())).with(Sort.by(Sort.Direction.DESC, "createdTime")), ApUserSearch.class);
        return ResponseResult.okResult(apUserSearches);

    }
}

