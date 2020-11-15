package com.xxl.job.core.handler.impl;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * glue任务处理器
 *
 * @author xuxueli 2016-5-19 21:05:45
 */
@AllArgsConstructor
public class GlueJobHandler extends IJobHandler {

  /**
   * 任务处理器
   */
  private IJobHandler jobHandler;
  /**
   * GLUE更新时间
   */
  @Getter
  private long glueUpdateTime;

  @Override
  public void execute()
      throws InterruptedException, IllegalAccessException, IOException, InvocationTargetException {
    XxlJobHelper.log("----------- glue.version:" + glueUpdateTime + " -----------");
    jobHandler.execute();
  }
}