package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

/**
 * 对外访问接口
 */
public interface TaskService {

    /**
     * 添加任务
     * @param task   任务对象
     * @return       任务id
     */
    public long addTask(Task task) ;


    /**
     *
     * @param taskId 任务id
     * @return
     */
    boolean cancelTask(long taskId);

    /**
     * 按照类型,和优先级进行拉取任务
     * @param type
     * @param priority
     * @return
     */
    Task poll(int type,int priority);

}