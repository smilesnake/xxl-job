package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.complete.XxlJobCompleter;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 任务丢失监视器实例
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
@Slf4j
public class JobCompleteHelper {

  private static JobCompleteHelper instance = new JobCompleteHelper();

  public static JobCompleteHelper getInstance() {
    return instance;
  }

  // ---------------------- monitor ----------------------
  /**
   * 回调线程.
   */
  private ThreadPoolExecutor callbackThreadPool = null;
  /**
   * 监听线程.
   */
  private Thread monitorThread;
  /**
   * 是否停止
   */
  private volatile boolean toStop = false;

  public void start() {

    // 回调线程池
    callbackThreadPool = new ThreadPoolExecutor(2, 20, 30L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(3000), r -> new Thread(r,
        "xxl-job, admin JobLosedMonitorHelper-callbackThreadPool-" + r.hashCode()),
        (r, executor) -> {
          r.run();
          log.warn(
              ">>>>>>>>>>> xxl-job, callback too fast, match threadpool rejected handler(run now).");
        });

    // 监听
    monitorThread = new Thread(() -> {

      // 等待JobTriggerPoolHelper 初始化
      try {
        TimeUnit.MILLISECONDS.sleep(50);
      } catch (InterruptedException e) {
        if (!toStop) {
          log.error(e.getMessage(), e);
        }
      }

      // monitor
      while (!toStop) {
        try {
          // 任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；
          LocalDateTime losedTime = LocalDateTime.now().plusMinutes(-10);
          List<Long> losedJobIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao()
              .findLostJobIds(losedTime);

          // 调度成功但任务状态在"处理中"的状态超过10分钟，设置为失败
          if (!CollectionUtils.isEmpty(losedJobIds)) {
            for (Long logId : losedJobIds) {

              XxlJobLog jobLog = new XxlJobLog();
              jobLog.setId(logId);

              jobLog.setHandleTime(LocalDate.now());
              jobLog.setHandleCode(ReturnT.FAIL_CODE);
              jobLog.setHandleMsg(I18nUtil.getString("joblog_lost_fail"));

              XxlJobCompleter.updateHandleInfoAndFinish(jobLog);
            }

          }
        } catch (Exception e) {
          if (!toStop) {
            log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
          }
        }

        try {
          TimeUnit.SECONDS.sleep(60);
        } catch (Exception e) {
          if (!toStop) {
            log.error(e.getMessage(), e);
          }
        }

      }

      log.info(">>>>>>>>>>> xxl-job, JobLosedMonitorHelper stop");

    });
    monitorThread.setDaemon(true);
    monitorThread.setName("xxl-job, admin JobLosedMonitorHelper");
    monitorThread.start();
  }

  /**
   * 停止监听任务丢失任务
   */
  public void toStop() {
    toStop = true;

    // stop registryOrRemoveThreadPool
    callbackThreadPool.shutdownNow();

    // stop monitorThread (interrupt and wait)
    monitorThread.interrupt();
    try {
      monitorThread.join();
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }

  // ---------------------- helper ----------------------

  /**
   * 回调
   *
   * @param callbackParamList 回调参数列表
   * @return ReturnT.SUCCESS
   */
  public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {

    callbackThreadPool.execute(() -> {
      for (HandleCallbackParam handleCallbackParam : callbackParamList) {
        // 打印回调结果
        ReturnT<String> callbackResult = callback(handleCallbackParam);
        log.debug(
            ">>>>>>>>> JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
            (callbackResult.getCode() == ReturnT.SUCCESS_CODE ? "success" : "fail"),
            handleCallbackParam, callbackResult);
      }
    });

    return ReturnT.SUCCESS;
  }

  /**
   * 回调
   *
   * @param handleCallbackParam 处理回调参数
   * @return 回调成功，ReturnT.SUCCESS，否则ReturnT.FAIL_CODE
   */
  private ReturnT<String> callback(HandleCallbackParam handleCallbackParam) {
    // 验证任务日志项
    XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao()
        .load(handleCallbackParam.getLogId());
    if (log == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE, "log item not found.");
    }
    //避免重复回调、调度子任务等，大于0表示已经执行过了
    if (log.getHandleCode() > 0) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          "log repeate callback.");     // avoid repeat callback, trigger child job etc
    }

    // 处理结果消息
    StringBuffer handleMsg = new StringBuffer();
    if (log.getHandleMsg() != null) {
      handleMsg.append(log.getHandleMsg()).append("<br>");
    }
    // 处理回调消息
    if (handleCallbackParam.getHandleMsg() != null) {
      handleMsg.append(handleCallbackParam.getHandleMsg());
    }

    // 成功，保存日志
    log.setHandleTime(LocalDate.now());
    log.setHandleCode(handleCallbackParam.getHandleCode());
    log.setHandleMsg(handleMsg.toString());
    XxlJobCompleter.updateHandleInfoAndFinish(log);

    return ReturnT.SUCCESS;
  }


}
