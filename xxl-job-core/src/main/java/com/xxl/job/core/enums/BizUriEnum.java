package com.xxl.job.core.enums;

import com.alibaba.fastjson.JSON;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.IdleBeatParam;
import com.xxl.job.core.biz.model.KillParam;
import com.xxl.job.core.biz.model.LogParam;
import com.xxl.job.core.biz.model.Param;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务URI枚举.
 *
 * @author smilesnake
 */

@AllArgsConstructor
public enum BizUriEnum {

  /**
   * 心跳检测.
   */
  BEAT("beat", null),
  /**
   * 忙碌检测
   */
  IDLE_BEAT("idleBeat", IdleBeatParam.class),
  /**
   * 调度任务执行
   */
  RUN("run", TriggerParam.class),
  /**
   * 终止任务
   */
  KILL("kill", KillParam.class),
  /**
   * 查看执行日志
   */
  LOG("log", LogParam.class);

  /**
   * uri.
   */
  @Getter
  private String uri;
  /**
   * 参数
   */
  private Class<?> cls;

  public static BizUriEnum match(String uri) {
    //过滤URI中的"/uri"的“/”
    uri = uri.substring(1);
    for (BizUriEnum item : BizUriEnum.values()) {
      if (item.uri.equals(uri)) {
        return item;
      }
    }
    return null;
  }

  public ReturnT mapper(ExecutorBiz executorBiz, String data) {
    Param param = (Param) JSON.parseObject(data, this.cls);
    switch (this) {
      case IDLE_BEAT:
        return executorBiz.idleBeat(param);
      case RUN:
        return executorBiz.run(param);
      case KILL:
        return executorBiz.kill(param);
      case LOG:
        return executorBiz.log(param);
      default:
        return executorBiz.beat();
    }
  }
}
