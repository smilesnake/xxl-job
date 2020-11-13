package com.xxl.job.admin.core.alarm;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 任务报警器
 *
 * @author xuxueli
 */
@Slf4j
@Component
public class JobAlarmer implements ApplicationContextAware, InitializingBean {
    /**
     * applicationContext对象
     */
    private ApplicationContext applicationContext;
    /**
     * 报警器列表.
     */
    private List<JobAlarm> jobAlarmList;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //获取Map对象的key为JobAlarm.class的实现类的名称，
        // 如：XxlJobService  --> com.xxl.job.admin.service.impl.XxlJobServiceImpl
        // 如：JobAlarmer  --> com.xxl.job.admin.core.alarm.impl.EmailJobAlarm
        Map<String, JobAlarm> serviceBeanMap = applicationContext.getBeansOfType(JobAlarm.class);
        // 加入报警列表
        if (!CollectionUtils.isEmpty(serviceBeanMap)) {
            jobAlarmList = new ArrayList<>(serviceBeanMap.values());
        }
    }

    /**
     * 任务报警
     *
     * @param info   任务信息
     * @param jobLog 任务日志
     * @return 报警成功，true,报警失败,false 如 -->EmailJobAlarm： 发送成功,true,否则，失败
     */
    public boolean alarm(XxlJobInfo info, XxlJobLog jobLog) {

        boolean result = false;
        if (!CollectionUtils.isEmpty(jobAlarmList)) {
            // success means all-success
            result = true;
            for (JobAlarm alarm : jobAlarmList) {
                boolean resultItem = false;
                try {
                    resultItem = alarm.doAlarm(info, jobLog);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
                if (!resultItem) {
                    result = false;
                }
            }
        }

        return result;
    }

}
