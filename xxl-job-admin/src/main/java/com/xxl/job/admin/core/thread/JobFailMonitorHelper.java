package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 失败任务监听器（job monitor instance）
 *
 * @author xuxueli 2015-9-1 18:05:56
 */
@Slf4j
public class JobFailMonitorHelper {


    private JobFailMonitorHelper() {
    }

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance() {
        return instance;
    }

    // ---------------------- monitor ----------------------
    /**
     * 监听线程.
     */
    private Thread monitorThread;
    /**
     * 是否停止
     */
    private volatile boolean toStop = false;

    public void start() {
        monitorThread = new Thread(() -> {
            // monitor
            while (!toStop) {
                try {
                    //查询失败的任务日志id
                    List<Long> failLogIds = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().findFailJobLogIds(1000);
                    if (!CollectionUtils.isEmpty(failLogIds)) {
                        for (long failLogId : failLogIds) {
                            // 日志锁
                            int lockRet = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, 0, -1);
                            if (lockRet < 1) {
                                continue;
                            }
                            XxlJobLog log = XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().load(failLogId);
                            XxlJobInfo info = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(log.getJobId());

                            // 1、失败重新监听
                            if (log.getExecutorFailRetryCount() > 0) {
                                JobTriggerPoolHelper.trigger(log.getJobId(), TriggerTypeEnum.RETRY, (log.getExecutorFailRetryCount() - 1), log.getExecutorShardingParam(), log.getExecutorParam(), null);
                                String retryMsg = "<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>" + I18nUtil.getString("jobconf_trigger_type_retry") + "<<<<<<<<<<< </span><br>";
                                log.setTriggerMsg(log.getTriggerMsg() + retryMsg);
                                // 更新任务调度日志信息
                                XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(log);
                            }

                            // 2、报警失败监听
                            // 告警状态：0-默认、-1=锁定状态、1-无需告警、2-告警成功、3-告警失败
                            int newAlarmStatus = 0;
                            if (info != null && StringUtils.isNotBlank(info.getAlarmEmail())) {
                                boolean alarmResult = XxlJobAdminConfig.getAdminConfig().getJobAlarmer().alarm(info, log);
                                newAlarmStatus = alarmResult ? 2 : 3;
                            } else {
                                newAlarmStatus = 1;
                            }
                            // 更新状态
                            XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateAlarmStatus(failLogId, -1, newAlarmStatus);
                        }
                    }

                } catch (Exception e) {
                    if (!toStop) {
                        log.error(">>>>>>>>>>> xxl-job, job fail monitor thread error:{}", e);
                    }
                }

                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (Exception e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }

            }
            log.info(">>>>>>>>>>> xxl-job, job fail monitor thread stop");
        });
        monitorThread.setDaemon(true);
        monitorThread.setName("xxl-job, admin JobFailMonitorHelper");
        monitorThread.start();
    }

    /**
     * 停止监听失败任务
     */
    public void toStop() {
        toStop = true;
        // interrupt and wait
        monitorThread.interrupt();
        try {
            monitorThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

}
