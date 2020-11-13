package com.xxl.job.core.biz.impl;

import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.*;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.impl.GlueJobHandler;
import com.xxl.job.core.handler.impl.ScriptJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.thread.JobThread;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * 内部业务执行器api调度实现层.
 *
 * @author xuxueli
 * @date 17/3/1
 */
@Slf4j
public class ExecutorBizImpl implements ExecutorBiz {

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(Param param) {
        IdleBeatParam idleBeatParam = (IdleBeatParam) param;
        // isRunningOrHasQueue
        boolean isRunningOrHasQueue = false;
        //根据jobId从任务线程仓库ConcurrentMap<cInteger, JobThread> jobThreadRepository中获取对应的任务线程
        JobThread jobThread = XxlJobExecutor.loadJobThread(idleBeatParam.getJobId());
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            isRunningOrHasQueue = true;
        }
        if (isRunningOrHasQueue) {
            return new ReturnT<>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> run(Param param) {
        TriggerParam triggerParam = (TriggerParam) param;
        // load old：jobHandler + jobThread
        //根据jobId从任务线程仓库ConcurrentMap<Integer, JobThread> jobThreadRepository中获取对应的任务线程；
        JobThread jobThread = XxlJobExecutor.loadJobThread(triggerParam.getJobId());
        // 任务处理器
        IJobHandler jobHandler = jobThread != null ? jobThread.getHandler() : null;
        String removeOldReason = null;

        // 验证jobHandler 和 jobThread
        GlueTypeEnum glueTypeEnum = GlueTypeEnum.match(triggerParam.getGlueType());

        // bean模式
        if (GlueTypeEnum.BEAN == glueTypeEnum) {

            // new jobhandler
            IJobHandler newJobHandler = XxlJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());

            // valid old jobThread
            // 如果jobThread不为空并且任务具体实例和从任务线程中拿到的任务实例不同则必须更改任务类型并且终止旧的任务线程
            if (jobThread != null && jobHandler != newJobHandler) {
                // 改变线程需要kill旧线程
                removeOldReason = "change jobhandler or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // 验证处理器
            if (jobHandler == null) {
                jobHandler = newJobHandler;
                if (jobHandler == null) {
                    return new ReturnT<>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }

        } else if (GlueTypeEnum.GLUE_GROOVY == glueTypeEnum) {
            // GLUE模式(Java) 任务以源码方式维护在调度中心；该模式的任务实际上是一段继承自IJobHandler的Java类代码并 "groovy" 源码方式维护，它在执行器项目中运行，可使用@Resource/@Autowire注入执行器里中的其他服务；

            // valid old jobThread
            // 如果jobThread不为空并且任务具体实例和从任务线程中拿到的任务实例不同则必须更改任务类型并且终止旧的任务线程
            if (jobThread != null && !(jobThread.getHandler() instanceof GlueJobHandler
                    && ((GlueJobHandler) jobThread.getHandler()).getGlueUpdateTime() == triggerParam.getGlueUpdatetime())) {
                // change handler or gluesource updated, need kill old thread
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // 验证处理器
            if (jobHandler == null) {
                try {
                    // 如果jobHandler为空，就通过Glue源码生成处理器对象
                    IJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                } catch (IllegalAccessException | InstantiationException e) {
                    log.error(e.getMessage(), e);
                    return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
                }
            }
        } else if (glueTypeEnum != null && glueTypeEnum.isScript()) {
            // glue非GLUE_GROOVY模式
            // valid old jobThread
            if (jobThread != null && !(jobThread.getHandler() instanceof ScriptJobHandler
                    && ((ScriptJobHandler) jobThread.getHandler()).getGlueUpdateTime() == triggerParam.getGlueUpdatetime())) {
                // change script or gluesource updated, need kill old thread
                // 如果jobThread不为空并且任务具体实例和从任务线程中拿到的任务实例不同则必须更改任务类型并且终止旧的任务线程
                removeOldReason = "change job source or glue type, and terminate the old job thread.";

                jobThread = null;
                jobHandler = null;
            }

            // 验证处理器,为空直接创建一个新的
            if (jobHandler == null) {
                jobHandler = new ScriptJobHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            }
            //至此，jobHandler不可能为空，因为为空就直接创建一个新的了
        } else {
            //只有以上三种情况
            return new ReturnT<>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        // 执行器阻塞策略
        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
        if (jobThread != null) {
            // 当任务正在运行时，丢弃
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy && jobThread.isRunningOrHasQueue()) {
                return new ReturnT<>(ReturnT.FAIL_CODE, "block strategy effect：" + ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());

            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy && Objects.requireNonNull(jobThread).isRunningOrHasQueue()) {
                // 杀死正在运行的线程
                jobThread = null;
            }
            // 只需排队调用

        }

        //  替换为新线程，旧线程移除
        if (jobThread == null) {
            jobThread = XxlJobExecutor.registerJobThread(triggerParam.getJobId(), jobHandler, removeOldReason);
        }

        // 添加调度参数到调度队列
        return jobThread.pushTriggerQueue(triggerParam);
    }

    @Override
    public ReturnT<String> kill(Param param) {
        KillParam killParam = (KillParam) param;
        // 杀死处理器线程
        JobThread jobThread = XxlJobExecutor.loadJobThread(killParam.getJobId());
        if (jobThread != null) {
            XxlJobExecutor.removeJobThread(killParam.getJobId(), "scheduling center kill job.");
            return ReturnT.SUCCESS;
        }
        return new ReturnT<>(ReturnT.SUCCESS_CODE, "job thread already killed.");
    }

    @Override
    public ReturnT<LogResult> log(Param param) {
        LogParam logParam = (LogParam) param;
        // log filename: logPath/yyyy-MM-dd/9999.log
        String logFileName = XxlJobFileAppender.makeLogFileName(Instant.ofEpochMilli(logParam.getLogDateTim())
                .atZone(ZoneOffset.ofHours(8)).toLocalDate(), logParam.getLogId());

        LogResult logResult = XxlJobFileAppender.readLog(logFileName, logParam.getFromLineNum());
        return new ReturnT<>(logResult);
    }

}
