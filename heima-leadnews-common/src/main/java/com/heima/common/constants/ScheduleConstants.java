package com.heima.common.constants;

public class ScheduleConstants {

    //task状态
    public static final int SCHEDULED=0;   //初始化状态

    public static final int EXECUTED=1;       //已执行状态

    public static final int CANCELLED=2;   //已取消状态

    public static final String FUTURE="future_";   //未来数据key前缀：zset集合

    public static final String TOPIC="topic_";     //当前数据key前缀:list集合
}