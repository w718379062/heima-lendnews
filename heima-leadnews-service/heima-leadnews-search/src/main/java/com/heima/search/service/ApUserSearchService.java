package com.heima.search.service;


import com.heima.model.common.dtos.ResponseResult;

public interface ApUserSearchService {
    /**
     *  存储用户搜索记录
     *
     * @param keyword 搜索关键词
     * @param userId 用户id
     * @return
     */
    public void insert(String keyword, Integer userId);
    /**
     查询搜索历史
     @return
     */
    ResponseResult findUserSearch();
}
