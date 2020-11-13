package com.xxl.job.admin.core.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/**
 * xxl-job 日志,用于跟踪触发进程
 *
 * @author xuxueli  2015-12-19 23:19:09
 */
@Data
public class XxlJobLog {
    /**
     * 主键id
     */
    private long id;

    // 任务信息
    /**
     * 执行器主键id
     */
    private int jobGroup;
    /**
     * 任务主键ID
     */
    private int jobId;

    // 执行信息
    /**
     * 执行器地址，本次执行的地址
     */
    private String executorAddress;
    /**
     * 执行器任务handler
     */
    private String executorHandler;
    /**
     * 执行器任务参数
     */
    private String executorParam;
    /**
     * 执行器任务分片参数，格式如 1/2
     */
    private String executorShardingParam;
    /**
     * 失败重试次数
     */
    private int executorFailRetryCount;

    // 调度信息
    /**
     * 调度-时间
     */
    private Date triggerTime;
    /**
     * 调度-结果
     */
    private int triggerCode;
    /**
     * 调度-日志
     */
    private String triggerMsg;

    // 处理信息
    /**
     * 执行-时间
     */
    private LocalDate handleTime;
    /**
     * 执行-状态
     */
    private int handleCode;
    /**
     * 执行-日志
     */
    private String handleMsg;

    // 告警状态信息
    /**
     * 告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败
     */
    private int alarmStatus;
}