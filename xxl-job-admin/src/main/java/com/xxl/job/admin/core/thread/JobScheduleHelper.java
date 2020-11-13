package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * 定时任务调度器帮助类
 *
 * @author xuxueli 2019-05-21
 */
@Slf4j
public class JobScheduleHelper {

  private JobScheduleHelper() {
  }

  private static JobScheduleHelper instance = new JobScheduleHelper();

  public static JobScheduleHelper getInstance() {
    return instance;
  }

  /**
   * 预读的毫秒数（5秒）
   */
  public static final long PRE_READ_MS = TimeUnit.SECONDS.toMillis(5);
  /**
   * 调度线程
   */
  private Thread scheduleThread;
  /**
   * 时间轮线程
   */
  private Thread ringThread;
  /**
   * 调度线程是否停止.
   */
  private volatile boolean scheduleThreadToStop = false;
  /**
   * 时间轮线程.
   */
  private volatile boolean ringThreadToStop = false;
  /**
   * 时间轮数据<秒数(1-60),jobId>
   */
  private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

  public void start() {

    // 调度线程
    scheduleThread = new Thread(() -> {

      try {
        //针对多个节点，防止调度器出现并发调度一个任务，调度器执行频率控制，最长睡眠5秒，最短4秒多点，即(5000-999)毫秒
        //如A节点的调度器刚启动，并且获取一个任务，然后加锁，如果同时B节点也启动，也获取到这个任务，防止重复调用，随机睡眠
        TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
      } catch (InterruptedException e) {
        if (!scheduleThreadToStop) {
          log.error(e.getMessage(), e);
        }
      }
      log.info(">>>>>>>>> init xxl-job admin scheduler success.");

      // pre-read count: treadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20)
      int preReadCount =
          (XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + XxlJobAdminConfig
              .getAdminConfig().getTriggerPoolSlowMax()) * 20;

      while (!scheduleThreadToStop) {

        // 扫描任务
        long start = System.currentTimeMillis();

        Connection conn = null;
        Boolean connAutoCommit = null;
        PreparedStatement preparedStatement = null;
        //如果5s内没有调度任务,false,预计失败，否则,预读成功
        boolean preReadSuc = true;
        try {

          conn = XxlJobAdminConfig.getAdminConfig().getDataSource().getConnection();
          connAutoCommit = conn.getAutoCommit();
          conn.setAutoCommit(false);
          //for update是在数据库中上锁用的，可以为数据库中的行上一个排它锁。当一个事务的操作未完成时候，其他事务可以读取但是不能写入或更新。
          preparedStatement = conn.prepareStatement(
              "select * from xxl_job_lock where lock_name = 'schedule_lock' for update");
          preparedStatement.execute();

          // tx start
          // 1、预读5s内调度任务
          long nowTime = System.currentTimeMillis();
          List<XxlJobInfo> scheduleList = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao()
              .scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
          if (!CollectionUtils.isEmpty(scheduleList)) {
            // 2、推送时间轮
            for (XxlJobInfo jobInfo : scheduleList) {

              // 时间轮刻度计算
              if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                // 2.1、当前任务的调度时间已经超时5秒以上了，不在执行，直接计算下一次触发时间，之后更新
                log.warn(">>>>>>>>>>> xxl-job, schedule misfire, jobId = " + jobInfo.getId());

                // 1、misfire match
                MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum
                    .match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                if (MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum) {
                  // FIRE_ONCE_NOW 》 trigger
                  JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1,
                      null, null, null);
                  log.debug(
                      ">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId());
                }

                // 刷新下一次的触发时间
                refreshNextValidTime(jobInfo, new Date());

              } else if (nowTime > jobInfo.getTriggerNextTime()) {
                // 2.2、// 调度时间未超过5s：立即执行一次，当前时间开始计算下次触发时间 trigger-expire < 5s：direct-trigger && make next-trigger-time
                // 1、调度
                JobTriggerPoolHelper
                    .trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                log.debug(
                    ">>>>>>>>>>> xxl-job, schedule push trigger : jobId = " + jobInfo.getId());

                // 2、 刷新下一次的触发时间
                refreshNextValidTime(jobInfo, new Date());

                //  next-trigger-time in 5s, pre-read again
                // 下一次的调度时间未5s内：立即执行一次，当前时间开始计算下次触发时间
                if (jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo
                    .getTriggerNextTime()) {

                  // 1、生成时间轮Key(ring second)
                  int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                  // 2、放入时间轮MAP中
                  pushTimeRing(ringSecond, jobInfo.getId());

                  // 3、刷新下一次的触发时间
                  refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                }

              } else {
                // 2.3、trigger-pre-read：time-ring trigger && make next-trigger-time
                // 2.3、未过期：正常调度，计算下次触发时间
                // 1、生成时间轮Key(ring second)
                int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                // 2、放入时间轮Map中
                pushTimeRing(ringSecond, jobInfo.getId());

                // 3、刷新下一次的触发时间
                refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

              }

            }

            // 3、更新trigger信息
            for (XxlJobInfo jobInfo : scheduleList) {
              XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleUpdate(jobInfo);
            }

          } else {
            preReadSuc = false;
          }

          // tx stop

        } catch (Exception e) {
          if (!scheduleThreadToStop) {
            log.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread error:{}", e);
          }
        } finally {

          // commit
          if (conn != null) {
            try {
              conn.commit();
            } catch (SQLException e) {
              if (!scheduleThreadToStop) {
                log.error(e.getMessage(), e);
              }
            }
            try {
              conn.setAutoCommit(connAutoCommit);
            } catch (SQLException e) {
              if (!scheduleThreadToStop) {
                log.error(e.getMessage(), e);
              }
            }
            try {
              conn.close();
            } catch (SQLException e) {
              if (!scheduleThreadToStop) {
                log.error(e.getMessage(), e);
              }
            }
          }

          // close PreparedStatement
          if (null != preparedStatement) {
            try {
              preparedStatement.close();
            } catch (SQLException e) {
              if (!scheduleThreadToStop) {
                log.error(e.getMessage(), e);
              }
            }
          }
        }
        //花费的时间
        long cost = System.currentTimeMillis() - start;

        // 如果执行的时间比较快，执行时间小于1秒，为了保证时间Wait seconds, align second
        if (cost < 1000) {  // scan-overtime, not wait
          try {
            // pre-read period: success > scan each second; fail > skip this period;
            // 为了时间同步，5s内没有调度任务,间隔为5秒，否则为1秒，
            TimeUnit.MILLISECONDS.sleep((preReadSuc ? TimeUnit.SECONDS.toMillis(1) : PRE_READ_MS)
                - System.currentTimeMillis() % 1000);
          } catch (InterruptedException e) {
            if (!scheduleThreadToStop) {
              log.error(e.getMessage(), e);
            }
          }
        }

      }

      log.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#scheduleThread stop");
    });
    scheduleThread.setDaemon(true);
    scheduleThread.setName("xxl-job, admin JobScheduleHelper#scheduleThread");
    scheduleThread.start();

    // 时间轮线程
    ringThread = new Thread(() -> {

      // align second
      // 时间同步
      try {
        TimeUnit.MILLISECONDS
            .sleep(TimeUnit.SECONDS.toMillis(1) - System.currentTimeMillis() % 1000);
      } catch (InterruptedException e) {
        if (!ringThreadToStop) {
          log.error(e.getMessage(), e);
        }
      }

      while (!ringThreadToStop) {
        try {
          // 时刻数据
          List<Integer> ringItemData = new ArrayList<>();
          int nowSecond = LocalDateTime.now().getSecond();
          // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
          for (int i = 0; i < 2; i++) {
            List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
            if (tmpData != null) {
              ringItemData.addAll(tmpData);
            }
          }

          // 时间轮 - 调度
          log.debug(">>>>>>>>>>> xxl-job, time-ring beat : " + nowSecond + " = " + Arrays
              .asList(ringItemData));
          if (ringItemData.size() > 0) {
            // 循环调度时间轮中任务
            for (int jobId : ringItemData) {
              // do trigger
              JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
            }
            // 调度完了,同时清空list列表
            ringItemData.clear();
          }
        } catch (Exception e) {
          if (!ringThreadToStop) {
            log.error(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread error:{}", e);
          }
        }

        // 下一次调度的秒数，时间同步(next second, align second）
        try {
          TimeUnit.MILLISECONDS
              .sleep(TimeUnit.SECONDS.toMillis(1) - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {
          if (!ringThreadToStop) {
            log.error(e.getMessage(), e);
          }
        }
      }
      log.info(">>>>>>>>>>> xxl-job, JobScheduleHelper#ringThread stop");
    });
    ringThread.setDaemon(true);
    ringThread.setName("xxl-job, admin JobScheduleHelper#ringThread");
    ringThread.start();
  }

  /**
   * 刷新下一次的触发时间
   *
   * @param jobInfo  任务信息
   * @param fromTime 开始时间
   * @throws ParseException cron解析错误，抛出
   */
  private void refreshNextValidTime(XxlJobInfo jobInfo, Date fromTime) throws ParseException {
    Date nextValidTime = generateNextValidTime(jobInfo, fromTime);
    if (nextValidTime != null) {
      jobInfo.setTriggerLastTime(jobInfo.getTriggerNextTime());
      jobInfo.setTriggerNextTime(nextValidTime.getTime());
    } else {
      jobInfo.setTriggerStatus(0);
      jobInfo.setTriggerLastTime(0L);
      jobInfo.setTriggerNextTime(0L);
    }
  }

  /**
   * 放入时间轮.(key存在add，不存在直接put)
   *
   * @param ringSecond 时间轮的key
   * @param jobId      任务id
   */
  private void pushTimeRing(int ringSecond, int jobId) {
    // push async ring
    List<Integer> ringItemData = ringData.get(ringSecond);
    if (ringItemData == null) {
      ringItemData = new ArrayList<>();
      ringData.put(ringSecond, ringItemData);
    }
    ringItemData.add(jobId);

    log.debug(">>>>>>>>>>> xxl-job, schedule push time-ring : " + ringSecond + " = " + Arrays
        .asList(ringItemData));
  }

  public void toStop() {

    // 1、停止调度
    scheduleThreadToStop = true;
    try {
      // wait
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    if (scheduleThread.getState() != Thread.State.TERMINATED) {
      // interrupt and wait
      scheduleThread.interrupt();
      try {
        scheduleThread.join();
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }

    // 时间轮中是否有数据
    boolean hasRingData = false;
    if (!ringData.isEmpty()) {
      for (int second : ringData.keySet()) {
        List<Integer> tmpData = ringData.get(second);
        if (!CollectionUtils.isEmpty(tmpData)) {
          hasRingData = true;
          break;
        }
      }
    }

    //如果有数据，
    if (hasRingData) {
      try {
        TimeUnit.SECONDS.sleep(8);
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }

    // stop ring (wait job-in-memory stop)
    // 停止时间轮（等待内存中的任务停止）
    ringThreadToStop = true;
    try {
      TimeUnit.SECONDS.sleep(1);
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
    //如果时间轮线程状态为终止状态，停止时间轮线程
    if (ringThread.getState() != Thread.State.TERMINATED) {
      // interrupt and wait
      ringThread.interrupt();
      try {
        ringThread.join();
      } catch (InterruptedException e) {
        log.error(e.getMessage(), e);
      }
    }
    log.info(">>>>>>>>>>> xxl-job, JobScheduleHelper stop");
  }
  // ---------------------- tools ----------------------

  /**
   * 生成下一次校验的时间
   *
   * @param jobInfo  任务信息
   * @param fromTime 开始的时间
   * @return 下一次校验的时间
   * @throws ParseException cron解析错误，抛出
   */
  public static Date generateNextValidTime(XxlJobInfo jobInfo, Date fromTime)
      throws ParseException {
    ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
    if (ScheduleTypeEnum.CRON == scheduleTypeEnum) {
      Date nextValidTime = new CronExpression(jobInfo.getScheduleConf())
          .getNextValidTimeAfter(fromTime);
      return nextValidTime;
    } else if (ScheduleTypeEnum.FIX_RATE
        == scheduleTypeEnum /*|| ScheduleTypeEnum.FIX_DELAY == scheduleTypeEnum*/) {
      return new Date(fromTime.getTime() + Integer.valueOf(jobInfo.getScheduleConf()) * 1000);
    }
    return null;
  }
}