package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;

/**
 * 任务报警接口
 *
 * @author xuxueli 2020-01-19
 */
public interface JobAlarm {

    /**
     * 任务报警.
     *
     * @param info   任务信息
     * @param jobLog 任务日志
     * @return 报警成功，true,报警失败,false 如 -->EmailJobAlarm： 发送成功,true,否则，失败
     */
    boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog);

}
