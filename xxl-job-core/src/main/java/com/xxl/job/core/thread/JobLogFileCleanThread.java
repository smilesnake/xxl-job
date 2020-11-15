package com.xxl.job.core.thread;

import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.util.FileUtil;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;

/**
 * 任务文件清理线程
 *
 * @author xuxueli 2017-12-29 16:23:43
 */
@Slf4j
public class JobLogFileCleanThread {

  private static JobLogFileCleanThread instance = new JobLogFileCleanThread();

  public static JobLogFileCleanThread getInstance() {
    return instance;
  }

  /**
   * 本地线程.
   */
  private Thread localThread;
  /**
   * 是否尝试停止.
   */
  private volatile boolean toStop = false;

  /**
   * 开始执行.
   *
   * @param logRetentionDays 日志保留天数
   */
  public void start(final long logRetentionDays) {
    // 最小保留三天
    final int retentionMinDay = 3;
    if (logRetentionDays < retentionMinDay) {
      return;
    }

    localThread = new Thread(() -> {
      while (!toStop) {
        // 超过日志保留天数,清除日志目录
        File[] childDirs = new File(XxlJobFileAppender.getLogBasePath()).listFiles();
        if (ArrayUtils.isNotEmpty(childDirs)) {
          for (File childFile : childDirs) {
            // valid
            if (!childFile.isDirectory() || !childFile.getName().contains("-")) {
              continue;
            }
            // 文件创建日期
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate logFileCreateDate = LocalDate.parse(childFile.getName(), dtf);
            if (logFileCreateDate == null) {
              continue;
            }
            //超出回收天数，删除文件
            if (logFileCreateDate.until(LocalDate.now(), ChronoUnit.DAYS) >= logRetentionDays) {
              FileUtil.deleteRecursively(childFile);
            }
          }
        }
        try {
          TimeUnit.DAYS.sleep(1);
        } catch (InterruptedException e) {
          if (!toStop) {
            log.error(e.getMessage(), e);
          }
          Thread.currentThread().interrupt();
        }
      }
      log.info(">>>>>>>>>>> xxl-job, executor JobLogFileCleanThread thread destory.");

    });
    localThread.setDaemon(true);
    localThread.setName("xxl-job, executor JobLogFileCleanThread");
    localThread.start();
  }

  /**
   * 暂停.
   */
  public void toStop() {
    toStop = true;

    if (localThread == null) {
      return;
    }
    // 中断
    localThread.interrupt();
    try {
      // 在子线程调用了join(),只有等到子线程结束了主线程才能中断
      localThread.join();
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
      Thread.currentThread().interrupt();
    }
  }

}
