package com.xxl.job.core.biz.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 空闲检测参数
 * @author xuxueli 2020-04-11 22:27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdleBeatParam implements Param {

  private static final long serialVersionUID = 42L;

  /**
   * 任务id
   */
  private int jobId;

}