package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import com.xxl.job.admin.core.model.extend.LogReportExt;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 任务日志统计帮助类
 *
 * @author xuxueli 2019-11-22
 */
@Slf4j
public class JobLogReportHelper {

  private JobLogReportHelper() {
  }

  private static JobLogReportHelper instance = new JobLogReportHelper();

  public static JobLogReportHelper getInstance() {
    return instance;
  }

  /**
   * 统计日志线程.
   */
  private Thread logReportThread;
  /**
   * 是否停止.
   */
  private volatile boolean toStop = false;

  public void start() {
    logReportThread = new Thread(() -> {

      // 最后清理日志的时间
      long lastCleanLogTime = 0;

      while (!toStop) {

        // 1、日志统计信息刷新：3天内刷新日志统计信息
        try {
          for (int i = 0; i < 3; i++) {
            // today
            LocalDateTime todayFrom = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
            LocalDateTime todayTo = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

            // 每分钟刷新日志统计信息
            XxlJobLogReport xxlJobLogReport = new XxlJobLogReport();
            xxlJobLogReport.setTriggerDay(todayFrom);
            LogReportExt logReportExt = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao()
                .findLogReport(todayFrom, todayTo);
            xxlJobLogReport.setRunningCount(logReportExt.getTriggerDayCountRunning());
            xxlJobLogReport.setSucCount(logReportExt.getTriggerDayCountSuc());
            xxlJobLogReport.setFailCount(logReportExt.getTriggerDayCountFail());

            // 刷新统计信息
            int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobLogReportDao()
                .update(xxlJobLogReport);
            if (ret < 1) {
              XxlJobAdminConfig.getAdminConfig().getXxlJobLogReportDao().save(xxlJobLogReport);
            }
          }

        } catch (Exception e) {
          if (!toStop) {
            log.error(">>>>>>>>>>> xxl-job, job log report thread error:{}", e);
          }
        }

        // 2、日志清理：开关打开（logRetentionDays > 0 ) 并且 每天一次
        if (XxlJobAdminConfig.getAdminConfig().getLogRetentionDays() > 0
            && System.currentTimeMillis() - lastCleanLogTime > TimeUnit.DAYS.toMillis(1L)) {
          // 过期时间
          LocalDateTime clearBeforeTime = LocalDateTime.now()
              .plusMonths(-1L * XxlJobAdminConfig.getAdminConfig().getLogRetentionDays());

          // 清理过期日志
          List<Long> logIds;
          do {
            logIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao()
                .findClearLogIds(0, 0, clearBeforeTime, 0, 1000);
            if (!CollectionUtils.isEmpty(logIds)) {
              XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().clearLog(logIds);
            }
          } while (!CollectionUtils.isEmpty(logIds));

          // 更新清理时间
          lastCleanLogTime = System.currentTimeMillis();
        }

        try {
          TimeUnit.MINUTES.sleep(1);
        } catch (Exception e) {
          if (!toStop) {
            log.error(e.getMessage(), e);
          }
        }

      }

      log.info(">>>>>>>>>>> xxl-job, job log report thread stop");

    });
    logReportThread.setDaemon(true);
    logReportThread.setName("xxl-job, admin JobLogReportHelper");
    logReportThread.start();
  }

  /**
   * 停止监听失败任务
   */
  public void toStop() {
    toStop = true;
    // interrupt and wait
    logReportThread.interrupt();
    try {
      logReportThread.join();
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
    }
  }
}
