package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.Data;

/**
 * 调度参数
 *
 * @author  xuxueli on 16/7/22.
 */
@Data
public class TriggerParam implements Param {

  private static final long serialVersionUID = 42L;

  /**
   * 任务id
   */
  private int jobId;
  /**
   * 执行器任务handler
   */
  private String executorHandler;
  /**
   * 执行器任务参数
   */
  private String executorParams;
  /**
   * 执行器阻塞策略
   */
  private String executorBlockStrategy;
  /**
   * 执行器超时时间
   */
  private int executorTimeout;
  /**
   * 日志id
   */
  private long logId;
  /**
   * 调度-时间
   */
  private long logDateTime;
  /**
   * GLUE类型
   *
   * @see com.xxl.job.core.glue.GlueTypeEnum
   */
  private String glueType;
  /**
   * GLUE源码
   */
  private String glueSource;
  /**
   * GLUE更新时间
   */
  private long glueUpdatetime;
  /**
   * 广播下标
   */
  private int broadcastIndex;
  /**
   * 广播总计
   */
  private int broadcastTotal;
}