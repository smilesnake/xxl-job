package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.IdleBeatParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * （忙碌转移）：按照顺序依次进行空闲检测，第一个空闲检测成功的机器选定为目标执行器并发起调度
 *
 * @author xuxueli on 17/3/10.
 */
@Slf4j
public class ExecutorRouteBusyover extends ExecutorRouter {

  @Override
  public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
    StringBuilder idleBeatResultBuild = new StringBuilder();
    // 忙碌转移则是通过执行器发送消息判断该任务对应的线程是否处于执行状态。
    for (String address : addressList) {
      // beat
      ReturnT<String> idleBeatResult;
      try {
        ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
        idleBeatResult = Objects.requireNonNull(executorBiz)
            .idleBeat(new IdleBeatParam(triggerParam.getJobId()));
      } catch (Exception e) {
        log.error(e.getMessage(), e);
        idleBeatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
      }
      idleBeatResultBuild.append((idleBeatResultBuild.length() > 0) ? "<br><br>" : "")
          .append(I18nUtil.getString("jobconf_idleBeat")).append("：")
          .append("<br>address：").append(address)
          .append("<br>code：").append(idleBeatResult.getCode())
          .append("<br>msg：").append(idleBeatResult.getMsg());

      // beat success
      // 第一个空闲检测成功的机器选定为目标执行器并发起调度
      if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {
        idleBeatResult.setMsg(idleBeatResultBuild.toString());
        idleBeatResult.setContent(address);
        return idleBeatResult;
      }
    }

    return new ReturnT<>(ReturnT.FAIL_CODE, idleBeatResultBuild.toString());
  }

}
