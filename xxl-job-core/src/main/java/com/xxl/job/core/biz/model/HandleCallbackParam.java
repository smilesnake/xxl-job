package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回调处理理参数实体.
 *
 * @author xuxueli on 17/3/2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HandleCallbackParam implements Serializable {

  private static final long serialVersionUID = 42L;
  /**
   * 日志id
   */
  private long logId;
  /**
   * 调度-时间
   */
  private long logDateTim;
  /**
   * 处理结果码
   */
  private int handleCode;
  /**
   * 处理结果消息
   */
  private String handleMsg;
}