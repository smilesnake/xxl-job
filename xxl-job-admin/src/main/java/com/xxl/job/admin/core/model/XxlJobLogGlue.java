package com.xxl.job.admin.core.model;

import lombok.Data;

import java.util.Date;

/**
 * 任务脚本信息，用于Glue的任务日志，用于跟踪任务代码过程
 *
 * @author xuxueli 2016-5-19 17:57:46
 */
@Data
public class XxlJobLogGlue {
    /**
     * 脚本id.
     */
    private int id;
    /**
     * 任务id.
     */
    private int jobId;
    /**
     * GLUE类型.
     *
     * @see com.xxl.job.core.glue.GlueTypeEnum
     */
    private String glueType;
    /**
     * 脚本源码.
     */
    private String glueSource;
    /**
     * 脚本备注.
     */
    private String glueRemark;
    /**
     * 添加的时间
     */
    private Date addTime;
    /**
     * 更新的时间
     */
    private Date updateTime;
}
