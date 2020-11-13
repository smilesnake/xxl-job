package com.xxl.job.admin.service.impl;

import com.xxl.job.admin.core.cron.CronExpression;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLogReport;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.MisfireStrategyEnum;
import com.xxl.job.admin.core.scheduler.ScheduleTypeEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import com.xxl.job.admin.dao.XxlJobLogReportDao;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import java.text.MessageFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * xxl任务的最重要的任务行为
 *
 * @author xuxueli 2016-5-28 15:30:33
 */
@Slf4j
@Service
public class XxlJobServiceImpl implements XxlJobService {


  @Resource
  private XxlJobGroupDao xxlJobGroupDao;
  @Resource
  private XxlJobInfoDao xxlJobInfoDao;
  @Resource
  public XxlJobLogDao xxlJobLogDao;
  @Resource
  private XxlJobLogGlueDao xxlJobLogGlueDao;
  @Resource
  private XxlJobLogReportDao xxlJobLogReportDao;

  @Override
  public Map<String, Object> pageList(int start, int length, int jobGroup, int triggerStatus,
      String jobDesc, String executorHandler, String author) {

    // 分页列表
    List<XxlJobInfo> list = xxlJobInfoDao
        .pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
    int count = xxlJobInfoDao
        .pageListCount(jobGroup, triggerStatus, jobDesc, executorHandler, author);

    // package result
    Map<String, Object> maps = new HashMap<>();
    // 总记录数
    maps.put("recordsTotal", count);
    // 过滤后的总记录数
    maps.put("recordsFiltered", count);
    // 分页列表
    maps.put("data", list);
    return maps;
  }

  @Override
  public ReturnT<String> add(XxlJobInfo jobInfo) {
    // 验证
    XxlJobGroup group = xxlJobGroupDao.load(jobInfo.getJobGroup());
    if (group == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_choose") + I18nUtil
          .getString("jobinfo_field_jobgroup")));
    }
    if (StringUtils.isBlank(jobInfo.getJobDesc())) {
      return new ReturnT<>(ReturnT.FAIL_CODE, (I18nUtil.getString("system_please_input") + I18nUtil
          .getString("jobinfo_field_jobdesc")));
    }
    if (StringUtils.isBlank(jobInfo.getAuthor())) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
    }

    // 验证调度
    ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
    if (scheduleTypeEnum == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
    }
    if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
      if (jobInfo.getScheduleConf() == null || !CronExpression
          .isValidExpression(jobInfo.getScheduleConf())) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            "Cron" + I18nUtil.getString("system_unvalid"));
      }
    } else if (scheduleTypeEnum
        == ScheduleTypeEnum.FIX_RATE/* || scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
      if (jobInfo.getScheduleConf() == null) {
        return new ReturnT<String>(ReturnT.FAIL_CODE, (I18nUtil.getString("schedule_type")));
      }
      try {
        int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
        if (fixSecond < 1) {
          return new ReturnT<String>(ReturnT.FAIL_CODE,
              (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
      } catch (Exception e) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
      }
    }

    // 任务验证
    if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_gluetype") + I18nUtil.getString("system_unvalid")));
    }
    if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType()) && (
        jobInfo.getExecutorHandler() == null
            || jobInfo.getExecutorHandler().trim().length() == 0)) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("system_please_input") + "JobHandler"));
    }
    // 》fix "\r" in shell
    if (GlueTypeEnum.GLUE_SHELL == GlueTypeEnum.match(jobInfo.getGlueType())
        && jobInfo.getGlueSource() != null) {
      jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
    }

    // 策略验证
    if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil
              .getString("system_unvalid")));
    }
    if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
    }
    if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil
              .getString("system_unvalid")));
    }

    // ChildJobId valid
    // 子任务id验证
    if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
      String[] childJobIds = jobInfo.getChildJobId().split(",");
      for (String childJobIdItem : childJobIds) {
        if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(
            childJobIdItem)) {
          XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
          if (childJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE,
                MessageFormat.format(
                    (I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil
                        .getString("system_not_found")), childJobIdItem));
          }
        } else {
          return new ReturnT<>(ReturnT.FAIL_CODE, MessageFormat.format(
              (I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil
                  .getString("system_unvalid")), childJobIdItem));
        }
      }

      // join , avoid "xxx,,"
      String temp = "";
      for (String item : childJobIds) {
        temp += item + ",";
      }
      temp = temp.substring(0, temp.length() - 1);

      jobInfo.setChildJobId(temp);
    }

    // 添加至数据库
    jobInfo.setAddTime(new Date());
    jobInfo.setUpdateTime(new Date());
    jobInfo.setGlueUpdatetime(new Date());
    xxlJobInfoDao.save(jobInfo);
    if (jobInfo.getId() < 1) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_add") + I18nUtil.getString("system_fail")));
    }
    return new ReturnT<>(String.valueOf(jobInfo.getId()));
  }

  /**
   * 是否为数字
   *
   * @param str 字符串
   * @return true, 是数字，否则，false
   */
  private boolean isNumeric(String str) {
    try {
      int result = Integer.valueOf(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public ReturnT<String> update(XxlJobInfo jobInfo) {

    // 验证
    if (jobInfo.getJobDesc() == null || jobInfo.getJobDesc().trim().length() == 0) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("system_please_input") + I18nUtil
              .getString("jobinfo_field_jobdesc")));
    }
    if (jobInfo.getAuthor() == null || jobInfo.getAuthor().trim().length() == 0) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_field_author")));
    }

    // 调度验证
    ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum.match(jobInfo.getScheduleType(), null);
    if (scheduleTypeEnum == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
    }
    if (scheduleTypeEnum == ScheduleTypeEnum.CRON) {
      if (jobInfo.getScheduleConf() == null || !CronExpression
          .isValidExpression(jobInfo.getScheduleConf())) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            "Cron" + I18nUtil.getString("system_unvalid"));
      }
    } else if (scheduleTypeEnum
        == ScheduleTypeEnum.FIX_RATE /*|| scheduleTypeEnum == ScheduleTypeEnum.FIX_DELAY*/) {
      if (jobInfo.getScheduleConf() == null) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
      }
      try {
        int fixSecond = Integer.valueOf(jobInfo.getScheduleConf());
        if (fixSecond < 1) {
          return new ReturnT<String>(ReturnT.FAIL_CODE,
              (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
        }
      } catch (Exception e) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
      }
    }

    // 策略验证
    if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_executorRouteStrategy") + I18nUtil
              .getString("system_unvalid")));
    }
    if (MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("misfire_strategy") + I18nUtil.getString("system_unvalid")));
    }
    if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_executorBlockStrategy") + I18nUtil
              .getString("system_unvalid")));
    }

    //子任务验证
    if (jobInfo.getChildJobId() != null && jobInfo.getChildJobId().trim().length() > 0) {
      String[] childJobIds = jobInfo.getChildJobId().split(",");
      for (String childJobIdItem : childJobIds) {
        if (childJobIdItem != null && childJobIdItem.trim().length() > 0 && isNumeric(
            childJobIdItem)) {
          XxlJobInfo childJobInfo = xxlJobInfoDao.loadById(Integer.parseInt(childJobIdItem));
          if (childJobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE,
                MessageFormat.format(
                    (I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil
                        .getString("system_not_found")), childJobIdItem));
          }
        } else {
          return new ReturnT<String>(ReturnT.FAIL_CODE,
              MessageFormat.format(
                  (I18nUtil.getString("jobinfo_field_childJobId") + "({0})" + I18nUtil
                      .getString("system_unvalid")), childJobIdItem));
        }
      }

      // join , avoid "xxx,,"
      String temp = "";
      for (String item : childJobIds) {
        temp += item + ",";
      }
      temp = temp.substring(0, temp.length() - 1);

      jobInfo.setChildJobId(temp);
    }

    // 验证执行器
    XxlJobGroup jobGroup = xxlJobGroupDao.load(jobInfo.getJobGroup());
    if (jobGroup == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_jobgroup") + I18nUtil.getString("system_unvalid")));
    }

    // 验证数据是否存在stage job info
    XxlJobInfo existsJobInfo = xxlJobInfoDao.loadById(jobInfo.getId());
    if (existsJobInfo == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_not_found")));
    }

    // next trigger time (5s后生效，避开预读周期)
    long nextTriggerTime = existsJobInfo.getTriggerNextTime();
    //任务处于运行状态但是cron不相等,重新生成下一次调度时间
    boolean scheduleDataNotChanged =
        jobInfo.getScheduleType().equals(existsJobInfo.getScheduleType()) && jobInfo
            .getScheduleConf().equals(existsJobInfo.getScheduleConf());
    if (existsJobInfo.getTriggerStatus() == 1 && !scheduleDataNotChanged) {
      try {
        Date nextValidTime = JobScheduleHelper.generateNextValidTime(jobInfo,
            new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));
        if (nextValidTime == null) {
          return new ReturnT<>(ReturnT.FAIL_CODE,
              I18nUtil.getString("jobinfo_field_cron_never_fire"));
        }
        nextTriggerTime = nextValidTime.getTime();
      } catch (ParseException e) {
        log.error(e.getMessage(), e);
        return new ReturnT<>(ReturnT.FAIL_CODE,
            I18nUtil.getString("jobinfo_field_cron_unvalid") + " | " + e.getMessage());
      }
    }

    existsJobInfo.setJobGroup(jobInfo.getJobGroup());
    existsJobInfo.setJobDesc(jobInfo.getJobDesc());
    existsJobInfo.setAuthor(jobInfo.getAuthor());
    existsJobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
    existsJobInfo.setScheduleType(jobInfo.getScheduleType());
    existsJobInfo.setScheduleConf(jobInfo.getScheduleConf());
    existsJobInfo.setMisfireStrategy(jobInfo.getMisfireStrategy());
    existsJobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
    existsJobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
    existsJobInfo.setExecutorParam(jobInfo.getExecutorParam());
    existsJobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
    existsJobInfo.setExecutorTimeout(jobInfo.getExecutorTimeout());
    existsJobInfo.setExecutorFailRetryCount(jobInfo.getExecutorFailRetryCount());
    existsJobInfo.setChildJobId(jobInfo.getChildJobId());
    existsJobInfo.setTriggerNextTime(nextTriggerTime);

    // 更新
    xxlJobInfoDao.update(existsJobInfo);
    return ReturnT.SUCCESS;
  }

  @Override
  public ReturnT<String> remove(int id) {
    XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);
    if (xxlJobInfo == null) {
      return ReturnT.SUCCESS;
    }

    xxlJobInfoDao.delete(id);
    xxlJobLogDao.delete(id);
    xxlJobLogGlueDao.deleteByJobId(id);
    return ReturnT.SUCCESS;
  }

  @Override
  public ReturnT<String> start(int id) {
    XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);

    // valid
    ScheduleTypeEnum scheduleTypeEnum = ScheduleTypeEnum
        .match(xxlJobInfo.getScheduleType(), ScheduleTypeEnum.NONE);
    if (ScheduleTypeEnum.NONE == scheduleTypeEnum) {
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("schedule_type_none_limit_start")));
    }

    // 下一次的调度时间 (5s后生效，避开预读周期)
    long nextTriggerTime = 0;
    try {
      Date nextValidTime = JobScheduleHelper.generateNextValidTime(xxlJobInfo,
          new Date(System.currentTimeMillis() + JobScheduleHelper.PRE_READ_MS));

      if (nextValidTime == null) {
        return new ReturnT<String>(ReturnT.FAIL_CODE,
            (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
      }
      nextTriggerTime = nextValidTime.getTime();
    } catch (ParseException e) {
      log.error(e.getMessage(), e);
      return new ReturnT<String>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")));
    }

    xxlJobInfo.setTriggerStatus(1);
    xxlJobInfo.setTriggerLastTime(0L);
    xxlJobInfo.setTriggerNextTime(nextTriggerTime);

    xxlJobInfo.setUpdateTime(new Date());
    // 更新启动状态及相关信息
    xxlJobInfoDao.update(xxlJobInfo);
    return ReturnT.SUCCESS;
  }

  @Override
  public ReturnT<String> stop(int id) {
    XxlJobInfo xxlJobInfo = xxlJobInfoDao.loadById(id);

    xxlJobInfo.setTriggerStatus(0);
    xxlJobInfo.setTriggerLastTime(0L);
    xxlJobInfo.setTriggerNextTime(0L);

    xxlJobInfo.setUpdateTime(new Date());
    xxlJobInfoDao.update(xxlJobInfo);
    return ReturnT.SUCCESS;
  }

  @Override
  public Map<String, Object> dashboardInfo() {
    // 报告数量
    int jobInfoCount = xxlJobInfoDao.findAllCount();
    //日志记录
    int jobLogCount = 0;
    //日志成功的记录
    int jobLogSuccessCount = 0;
    XxlJobLogReport xxlJobLogReport = xxlJobLogReportDao.queryLogReportTotal();
    if (xxlJobLogReport != null) {
      jobLogCount = xxlJobLogReport.getRunningCount() + xxlJobLogReport.getSucCount() +
          xxlJobLogReport.getFailCount();
      jobLogSuccessCount = xxlJobLogReport.getSucCount();
    }

    // executor count
    //执行器的地址集
    Set<String> executorAddressSet = new HashSet<>();
    List<XxlJobGroup> groupList = xxlJobGroupDao.findAll();

    if (!CollectionUtils.isEmpty(groupList)) {
      for (XxlJobGroup group : groupList) {
        if (CollectionUtils.isEmpty(group.getRegistryList())) {
          executorAddressSet.addAll(group.getRegistryList());
        }
      }
    }
    // 执行器的数量
    int executorCount = executorAddressSet.size();

    Map<String, Object> dashboardMap = new HashMap<>();
    dashboardMap.put("jobInfoCount", jobInfoCount);
    dashboardMap.put("jobLogCount", jobLogCount);
    dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
    dashboardMap.put("executorCount", executorCount);
    return dashboardMap;
  }

  @Override
  public ReturnT<Map<String, Object>> chartInfo(Date startDate, Date endDate) {

    // process
    //调度的日期
    List<String> triggerDayList = new ArrayList<>();
    //调度运行中的数量
    List<Integer> triggerDayCountRunningList = new ArrayList<>();
    //调度成功的数量
    List<Integer> triggerDayCountSucList = new ArrayList<>();
    //调度失败的数量
    List<Integer> triggerDayCountFailList = new ArrayList<>();
    int triggerCountRunningTotal = 0;
    int triggerCountSucTotal = 0;
    int triggerCountFailTotal = 0;

    List<XxlJobLogReport> logReportList = xxlJobLogReportDao.queryLogReport(startDate, endDate);
    // 拼装数据
    if (!CollectionUtils.isEmpty(logReportList)) {
      for (XxlJobLogReport item : logReportList) {
        String day = DateUtil.formatDate(item.getTriggerDay());
        int triggerDayCountRunning = item.getRunningCount();
        int triggerDayCountSuc = item.getSucCount();
        int triggerDayCountFail = item.getFailCount();

        triggerDayList.add(day);
        triggerDayCountRunningList.add(triggerDayCountRunning);
        triggerDayCountSucList.add(triggerDayCountSuc);
        triggerDayCountFailList.add(triggerDayCountFail);

        triggerCountRunningTotal += triggerDayCountRunning;
        triggerCountSucTotal += triggerDayCountSuc;
        triggerCountFailTotal += triggerDayCountFail;
      }
    } else {
      for (int i = -6; i <= 0; i++) {
        triggerDayList.add(DateUtil.formatDate(DateUtil.addDays(LocalDateTime.now(), i)));
        triggerDayCountRunningList.add(0);
        triggerDayCountSucList.add(0);
        triggerDayCountFailList.add(0);
      }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("triggerDayList", triggerDayList);
    result.put("triggerDayCountRunningList", triggerDayCountRunningList);
    result.put("triggerDayCountSucList", triggerDayCountSucList);
    result.put("triggerDayCountFailList", triggerDayCountFailList);

    result.put("triggerCountRunningTotal", triggerCountRunningTotal);
    result.put("triggerCountSucTotal", triggerCountSucTotal);
    result.put("triggerCountFailTotal", triggerCountFailTotal);

    return new ReturnT<>(result);
  }

}
