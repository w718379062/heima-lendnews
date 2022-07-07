package com.heima.schedule.test;


import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest

public class TaskinfoLogsMapperTest {

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;

    @Test
    public void testInsert() {
        TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
        taskinfoLogs.setExecuteTime(new Date());
        taskinfoLogs.setPriority(1);
        taskinfoLogs.setTaskType(1);
        taskinfoLogs.setVersion(1);

        taskinfoLogsMapper.insert(taskinfoLogs);
    }

    @Test
    //测试乐观锁
    public void testOptimisticLocker() throws InterruptedException {
        //1.先把要修改的数据查询出来
        TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(1420369799178907649L);
        System.out.println("当前版本：" + taskinfoLogs.getVersion());

        //2.1 准备修改数据，
        taskinfoLogs.setExecuteTime(new Date());
        taskinfoLogs.setPriority(1000);

        //准备修改数据时，有其他线程修改了此行数据，相当于version发生了变化
        Thread.sleep(1000 * 10); //通过客户端修改了version

        //2.2 执行修改，如果此时数据库中version的值等于第一步查出来的值，则可以修改成功
        //实际执行SQL：
        //UPDATE taskinfo_logs SET execute_time=?, priority=?, version=? WHERE task_id=? AND version=?
        System.out.println("开始执行修改");
        int count = taskinfoLogsMapper.updateById(taskinfoLogs);
        System.out.print(count > 0 ? "修改成功！" : "修改失败！");
        System.out.println("修改之后版本：" + taskinfoLogs.getVersion());
    }
}