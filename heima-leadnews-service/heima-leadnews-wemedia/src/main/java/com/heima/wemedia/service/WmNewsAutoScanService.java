package com.heima.wemedia.service;

public interface WmNewsAutoScanService {

    /**
     * 自媒体文章审核
     * @param wmNewsId  自媒体文章id
     */
    public void autoScanWmNews(Integer wmNewsId); //wm_news.id
}