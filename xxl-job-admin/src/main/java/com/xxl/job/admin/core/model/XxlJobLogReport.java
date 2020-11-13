package com.xxl.job.admin.core.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务日志报告
 *
 * @author xuxueli
 */
@Data
public class XxlJobLogReport {
    /**
     * 主键id.
     */
    private int id;
    /**
     * 调度 - 时间.
     */
    private LocalDateTime triggerDay;
    /**
     * 运行中 - 日志数量.
     */
    private int runningCount;
    /**
     * 执行成功 - 日志数量.
     */
    private int sucCount;
    /**
     * 执行失败 - 日志数量.
     */
    private int failCount;
}
