package com.heima.schedule.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.CacheService;
import com.heima.common.constants.ScheduleConstants;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;

import com.heima.schedule.mapper.TaskinfoLogsMapper;

import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class TaskServiceimpl implements TaskService {
    @Autowired
    private TaskinfoMapper taskinfoMapper;

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    @Autowired
    private CacheService cacheService;

    /**
     * 保存任务到数据库中
     *
     * @param task 任务对象
     * @return
     */
    @Transactional//事务管理
    @Override
    public long addTask(Task task) {

        //添加任务到数据库中
        boolean success = addTaskTodb(task);
        //如果添加到数据库中的操作执行成功
        if (success) {
            //添加数据到redis中
            addtaskToCache(task);

        }


        return task.getTaskId();
    }

    /**
     * 取消任务
     * @param taskId 任务id
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        //
        boolean flag=false;
        //删除任务,更新日志
        //更新日志
        Task task = updateDb(taskId, ScheduleConstants.CANCELLED);

        //删除任务
        if (task!=null){
            //如果数据不为空则删除任务
            removeTaskFromCache(task);
        }

        return flag;
    }

    /**
     * 删除redis中的数据
     * @param task 任务数据
     */
    private void removeTaskFromCache(Task task) {
        //根据redis中的key删除
        //获取key
        String key=task.getTaskType()+"_"+task.getPriority();
        if (task.getExecuteTime()<=System.currentTimeMillis()){
            //从list中删除数据
            cacheService.lRemove(ScheduleConstants.TOPIC+key,0,JSON.toJSONString(task));

        }else {
            //从zset中删除
            cacheService.zRemove(ScheduleConstants.FUTURE + key, JSON.toJSONString(task));



        }

    }

    /**
     * 删除任务 taskInfo
     * 使用乐观锁更新任务日志
     *
     * @param taskId 任务 id
     * @param cancelled  任务状态 2 取消任务
     * @return
     */
    private Task updateDb(long taskId, int cancelled) {
        Task task=null;
        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);
            //更新任务日志表
            //使用乐观锁必须先查询数据在进行更新操作
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            //将状态更新为取消状态
            taskinfoLogs.setStatus(cancelled);
            //更新日志表,更新时需要携带版本号,乐观锁
            taskinfoLogsMapper.updateById(taskinfoLogs);
            task = new Task();
            //对象拷贝
            BeanUtils.copyProperties(taskinfoLogs,task);
            //修改执行时间
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());
        } catch (BeansException e) {
            log.error("task cancel exception taskId={}", taskId);
            e.printStackTrace();
        }
        return task;

    }

    /**
     * 把任务提交到redis中
     *
     * @param task
     */
    private void addtaskToCache(Task task) {
        //存放在redis中的key
        String key = task.getTaskType() + "_" + task.getPriority();
        /*注:System.currentTimeMillis() 获得的是自1970-1-01 00:00:00.000 到当前时刻的时间距离,类型为long。*/
        //获取五分钟之后的毫秒值
        Calendar instance = Calendar.getInstance();
        instance.add(Calendar.MINUTE, 5);
        long timeInMillis = instance.getTimeInMillis();
        //如果任务执行的时间小于等于当前时间存入redis
        if (task.getExecuteTime() <= System.currentTimeMillis()) {
            //序列化

            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));


        } else if (task.getExecuteTime() <= timeInMillis) {
            //如果任务大于当前时间,小于预设时间放入到zset中
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task), task.getExecuteTime());

        }

    }

    private boolean addTaskTodb(Task task) {
        boolean flag = false;
        try {
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task, taskinfo);
            //设置执行时间,要的是一个long类型的,需要转换
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            //执行插入
            taskinfoMapper.insert(taskinfo);
            //设置任务id
            task.setTaskId(taskinfo.getTaskId());
            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            //使用bean对象的拷贝
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            //设置版本号
            taskinfoLogs.setVersion(1);
            //设置状态 0初始化,1执行成功2取消执行
            //初始化状态
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);

            //执行sql
            taskinfoLogsMapper.insert(taskinfoLogs);
            flag = true;

        } catch (BeansException e) {
            e.printStackTrace();
        }

        return flag;
    }

    /**
     *按照类型和优先级拉取任务
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;
        try {
            String key = type+"_"+priority;
            //pop = get + delete
            String task_json = cacheService.lRightPop(ScheduleConstants.TOPIC + key);
            if(StringUtils.isNotBlank(task_json)){
                task = JSON.parseObject(task_json, Task.class);
                //更新数据库信息
                updateDb(task.getTaskId(),ScheduleConstants.EXECUTED);
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("poll task exception");
        }
        return task;
    }
    @Scheduled(cron = "0 */1 * * * ?") //每分钟执行一次
    public void refresh() {
        //解决集群下方法抢占执行
        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        if (StringUtils.isNotBlank(token)) {

            log.info("未来数据定时刷新---定时任务");
            //获取所有未来数据的集合key
            Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            for (String futureKey : futureKeys) {//future_100_50
                //按照key和分值查询符合条件的数据
                Set<String> tasks = cacheService.zRangeByScore(futureKey, 0,
                        System.currentTimeMillis());
                //同步数据
                if (!tasks.isEmpty()) {
                    //获取当前数据的key：topic开头
                    //"future_100_50".split("future_")[1] = "100_50" -> topic_100_50
                    String topicKey =
                            ScheduleConstants.TOPIC + futureKey.split(ScheduleConstants.FUTURE)[1];

                    //future_100_50 -> topic_100_50
                    //String topicKey = futureKey.replace(ScheduleConstants.FUTURE, ScheduleConstants.TOPIC);

                    cacheService.refreshWithPipeline(futureKey, topicKey, tasks);
                    log.info("成功的将" + futureKey + "刷新到了" + topicKey);
                }
            }
        }
    }
    @Scheduled(cron = "0 */5 * * * ?") //每五分钟同步一次
    @PostConstruct //项目启动，Spring初始化对象时执行
    public void reloadData() {
        // 清除缓存中原有的数据
        clearCache();
        log.info("数据库数据同步到缓存");
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 5);

        //查看小于未来5分钟的所有任务
        List<Taskinfo> allTasks = taskinfoMapper.selectList(
                Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, calendar.getTime()));
        if (allTasks != null && allTasks.size() > 0) {
            for (Taskinfo taskinfo : allTasks) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                //添加任务
                addTask(task);
            }
        }
    }
    /**
     * 清理缓存中的数据
     */
    private void clearCache() {
        // 删除缓存中未来数据集合和当前消费者队列的所有key
        // topic_
        Set<String> topickeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        // future_
        Set<String> futurekeys = cacheService.scan(ScheduleConstants.FUTURE + "*");

        cacheService.delete(futurekeys);
        cacheService.delete(topickeys);
    }
}
