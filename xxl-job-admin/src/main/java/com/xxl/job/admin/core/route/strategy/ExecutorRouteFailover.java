package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;

/**
 * （故障转移）：按照顺序依次进行心跳检测，第一个心跳检测成功的机器选定为目标执行器并发起调度
 *
 * @author xuxueli on 17/3/10.
 */
@Slf4j
public class ExecutorRouteFailover extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();
        //循环集群地址
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult;
            try {
                // 向执行器发送 执行beat信息,试探该机器是否可以正常工作
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                beatResult = Objects.requireNonNull(executorBiz).beat();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                beatResult = new ReturnT<>(ReturnT.FAIL_CODE, "" + e);
            }
            // 拼接日志,收集日志信息,后期一起返回
            beatResultSB.append((beatResultSB.length() > 0) ? "<br><br>" : "")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // 返回状态为成功
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {
                //拼装返回信息
                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<>(ReturnT.FAIL_CODE, beatResultSB.toString());

    }
}
