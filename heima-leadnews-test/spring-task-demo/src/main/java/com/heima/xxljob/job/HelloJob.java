package com.heima.xxljob.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class HelloJob {

    @Value("${server.port}") //tomcat运行端口
    private String port;
    @XxlJob("hellojob")
    public void hello(){
        System.out.println(new Date()+"执行了  sssssssssssssssssssssssss");
    }

    @XxlJob("shardingJobHandler")
    public void shardingJobHandler() {
        //分片的参数
        //1.当前分片序号(从0开始)，执行器集群列表中当前执行器的序号；
        int shardIndex = XxlJobHelper.getShardIndex(); //服务器的标识：0, 1 （从0开始的）
        //2.总分片数，执行器集群的总机器数量； total = 2
        int shardTotal = XxlJobHelper.getShardTotal(); //服务器的总数量

        //业务逻辑
        List<Integer> list = getList(); //拉取1w个任务
        for (Integer id : list) { // 任务ID：integer = 0 - 9999
            //任意数字 % 2 -》 0，1
            if (id % shardTotal==shardIndex) { //判断当前ID的任务是否由当前分片服务器执行
                System.out.println(new Date() + "当前第" + shardIndex + "分片（服务器）执行了，任务项为ssss：" + id);
            }
        }
    }
    //创建1W个任务
    public List<Integer> getList() {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            list.add(i);
        }
        return list;
    }
}
