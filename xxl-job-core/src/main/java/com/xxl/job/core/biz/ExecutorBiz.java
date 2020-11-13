package com.xxl.job.core.biz;

import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.Param;

/**
 * 执行业务（api调度执行器）.
 *
 * @author xuxueli
 * @date 17/3/1
 */
public interface ExecutorBiz {

    /**
     * 心跳检测：调度中心使用.
     *
     * @return 调度成功，ReturnT.SUCCESS 否则，ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> beat();

    /**
     * 忙碌检测：调度中心使用.
     *
     * @param idleBeatParam 空闲检测参数
     * @return 调度成功, 空闲状态，ReturnT.SUCCESS 否则，运行状态或者正在调度队列中，ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> idleBeat(Param idleBeatParam);

    /**
     * 调度任务执行：调度中心使用；本地进行任务开发时，可使用该API服务模拟触发任务.
     *
     * @param triggerParam 调度参数
     * @return 调度成功，ReturnT.SUCCESS 否则，ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> run(Param triggerParam);

    /**
     * 终止任务.
     *
     * @param killParam 终止参数
     * @return 调度成功，ReturnT.SUCCESS 否则，ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<String> kill(Param killParam);

    /**
     * 查看执行日志.
     *
     * @param logParam 日志参数
     * @return 调度成功，ReturnT.SUCCESS 否则，ReturnT.FAIL_CODE
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL_CODE
     */
    ReturnT<LogResult> log(Param logParam);

}
