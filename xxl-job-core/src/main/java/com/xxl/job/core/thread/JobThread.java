package com.xxl.job.core.thread;

import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobFileAppender;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;


/**
 * 处理器线程
 *
 * @author xuxueli 2016-1-16 19:52:47
 */
@Slf4j
public class JobThread extends Thread {

  /**
   * 任务id.
   */
  private int jobId;
  /**
   * 任务处理器
   */
  @Getter
  private IJobHandler handler;
  /**
   * 调度队列.
   */
  private LinkedBlockingQueue<TriggerParam> triggerQueue;
  /**
   * 调度日志id,使用Set避免对同一触发器日志标识重复触发器
   */
  private Set<Long> triggerLogIdSet;
  /**
   * 去暂停.
   */
  private volatile boolean toStop = false;
  /**
   * 停止原因
   */
  private String stopReason;
  /**
   * 是否正在运行
   */
  private boolean running = false;
  /**
   * 理想时间
   */
  private int idleTimes = 0;

  /**
   * 构造任务线程
   *
   * @param jobId   任务id
   * @param handler 任务处理器
   */
  public JobThread(int jobId, IJobHandler handler) {
    this.jobId = jobId;
    this.handler = handler;
    this.triggerQueue = new LinkedBlockingQueue<>();
    this.triggerLogIdSet = Collections.synchronizedSet(new HashSet<>());
  }

  /**
   * 添加调度参数到调度队列.
   *
   * @param triggerParam 调度参数
   * @return 重复，ReturnT.FAIL_CODE.否则，ReturnT.SUCCESS
   * @see ReturnT#FAIL_CODE
   * @see ReturnT#SUCCESS
   */
  public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam) {
    // avoid repeat
    //避免重复
    if (triggerLogIdSet.contains(triggerParam.getLogId())) {
      log.info(">>>>>>>>>>> repeate trigger job, logId:{}", triggerParam.getLogId());
      return new ReturnT<>(ReturnT.FAIL_CODE,
          "repeate trigger job, logId:" + triggerParam.getLogId());
    }

    triggerLogIdSet.add(triggerParam.getLogId());
    triggerQueue.add(triggerParam);
    return ReturnT.SUCCESS;
  }

  /**
   * 停止本线程.
   *
   * @param stopReason 停止原因
   */
  public void toStop(String stopReason) {
    // Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
    // 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
    // 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
    this.toStop = true;
    this.stopReason = stopReason;
  }

  /**
   * 是否正在运行任务（正在运行或者正在调度队列中).
   *
   * @return true, 正在运行或者正在调度队列中.否则，false
   */
  public boolean isRunningOrHasQueue() {
    return running || !triggerQueue.isEmpty();
  }

  @Override
  public void run() {

    // init
    try {
      handler.init();
    } catch (IllegalAccessException | InvocationTargetException e) {
      log.error(e.getMessage(), e);
    }

    // execute
    while (!toStop) {
      running = false;
      idleTimes++;

      TriggerParam triggerParam = null;
      try {
        // to check toStop signal, we need cycle, so wo cannot use queue.take(), instand of poll(timeout)
        // 为了检查停止信号，我们需要循环，所以我们不能使用queue.take()，而是poll(timeout)
        triggerParam = triggerQueue.poll(3L, TimeUnit.SECONDS);
        if (triggerParam != null) {
          running = true;
          idleTimes = 0;
          triggerLogIdSet.remove(triggerParam.getLogId());

          // 文件名称, like "logPath/yyyy-MM-dd/9999.log"
          String logFileName = XxlJobFileAppender.makeLogFileName(Instant.ofEpochMilli(triggerParam
                  .getLogDateTime()).atZone(ZoneOffset.ofHours(8)).toLocalDate(),
              triggerParam.getLogId());
          XxlJobContext xxlJobContext = new XxlJobContext(triggerParam.getJobId(),
              triggerParam.getExecutorParams(), logFileName, triggerParam.getBroadcastIndex(),
              triggerParam.getBroadcastTotal());

          // init job context
          XxlJobContext.setXxlJobContext(xxlJobContext);

          // 执行
          XxlJobHelper.log(String.format(
              "<br>----------- xxl-job job execute start -----------<br>----------- Param:%s",
              triggerParam.getExecutorParams()));
          //超时时间大于0
          if (triggerParam.getExecutorTimeout() > 0) {
            // limit timeout
            Thread futureThread = null;
            try {
              FutureTask<Boolean> futureTask = new FutureTask<>(() -> {
                // init job context
                XxlJobContext.setXxlJobContext(xxlJobContext);

                handler.execute();
                return true;
              });
              futureThread = new Thread(futureTask);
              futureThread.start();

              Boolean tempResult = futureTask
                  .get(triggerParam.getExecutorTimeout(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
              XxlJobHelper.log("<br>----------- xxl-job job execute timeout");
              XxlJobHelper.log(e);

              // handle result
              XxlJobHelper.handleTimeout("job execute timeout ");
            } finally {
              Objects.requireNonNull(futureThread).interrupt();
            }
          } else {
            // 超时时间小于等于0，立即执行
            handler.execute();
          }

          if (XxlJobContext.getXxlJobContext().getHandleCode() <= 0) {
            XxlJobHelper.handleFail("job handle result lost.");
          } else {
            // 格式化返回结果
            String tempHandleMsg = XxlJobContext.getXxlJobContext().getHandleMsg();
            tempHandleMsg = (tempHandleMsg != null && tempHandleMsg.length() > 50000)
                ? tempHandleMsg.substring(0, 50000).concat("...") : tempHandleMsg;
            XxlJobContext.getXxlJobContext().setHandleMsg(tempHandleMsg);
          }
          XxlJobHelper.log(String.format(
              "<br>----------- xxl-job job execute end(finish) -----------<br>----------- Result: handleCode=%d, handleMsg = %s",
              XxlJobContext.getXxlJobContext().getHandleCode(),
              XxlJobContext.getXxlJobContext().getHandleMsg()));
        } else {
          // 回调参数为空，调度队列也为空，并且空闲次数为30，移除这个任务id, 避免并发调度器导致jobId丢失,
          if (idleTimes > 30 && triggerQueue.isEmpty()) {
            XxlJobExecutor.removeJobThread(jobId, "excutor idel times over limit.");
          }
        }
      } catch (InterruptedException | IllegalAccessException | IOException |
          InvocationTargetException | ExecutionException e) {
        if (toStop) {
          XxlJobHelper.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
        }
        //返回异常信息
        try (StringWriter stringWriter = new StringWriter()) {
          e.printStackTrace(new PrintWriter(stringWriter));
          String errorMsg = stringWriter.toString();
          XxlJobHelper.handleFail(errorMsg);
          XxlJobHelper.log(String.format(
              "<br>----------- JobThread Exception:%s<br>----------- xxl-job job execute end(error) -----------",
              errorMsg));
        } catch (IOException ex) {
          log.error(ex.getMessage(), ex);
          Thread.currentThread().interrupt();
        }
      } finally {
        if (triggerParam != null) {
          // 回调处理器信息
          if (!toStop) {
            // commonm
            TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(),
                triggerParam.getLogDateTime(), XxlJobContext.getXxlJobContext().getHandleCode(),
                XxlJobContext.getXxlJobContext().getHandleMsg())
            );
          } else {
            // is killed
            // 已经被kill了
            TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(),
                triggerParam.getLogDateTime(), XxlJobContext.HANDLE_COCE_FAIL,
                stopReason + " [job running, killed]"));
          }
        }
      }
    }

    // callback trigger request in queue
    //队列中的回调调度器器请求
    while (!CollectionUtils.isEmpty(triggerQueue)) {
      TriggerParam triggerParam = triggerQueue.poll();
      if (triggerParam != null) {
        // 任务没有执行，还在任务队列中，被kill了
        TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(),
            triggerParam.getLogDateTime(), XxlJobContext.HANDLE_COCE_FAIL,
            stopReason + " [job not executed, in the job queue, killed.]")
        );
      }
      // 销毁
      try {
        handler.destroy();
      } catch (IllegalAccessException | InvocationTargetException e) {
        log.error(e.getMessage(), e);
      }
      log.info(">>>>>>>>>>> xxl-job JobThread stoped, hashCode:{}", Thread.currentThread());
    }
  }
}
