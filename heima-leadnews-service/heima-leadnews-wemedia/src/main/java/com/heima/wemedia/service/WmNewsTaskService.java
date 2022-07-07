package com.heima.wemedia.service;


import com.heima.model.wemedia.pojos.WmNews;
public interface WmNewsTaskService {
    /**
     * 添加任务到延迟队列中
     * @param wmNews
     */
    public void addNewsToTask(WmNews wmNews);

    /**
     *
     * 消费任务
     */
    public void scanNewsByTask();
}
