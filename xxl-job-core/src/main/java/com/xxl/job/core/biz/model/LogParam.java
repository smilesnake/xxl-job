package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 日志参数
 * @author xuxueli 2020-04-11 22:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogParam implements Param {

  private static final long serialVersionUID = 42L;

  /**
   * 调度-时间.
   */
  private long logDateTim;
  /**
   * 日志id.
   */
  private long logId;
  /**
   * 起始行号.
   */
  private int fromLineNum;

}