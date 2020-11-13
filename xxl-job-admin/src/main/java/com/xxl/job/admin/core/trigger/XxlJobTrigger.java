package com.xxl.job.admin.core.trigger;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.ThrowableUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.Date;

/**
 * xxl-job 调度器
 *
 * @author xuxueli on 17/7/13.
 */
@Slf4j
public class XxlJobTrigger {

  /**
   * 触发任务
   *
   * @param jobId                 任务id
   * @param triggerType           任务触发类型枚举
   * @param failRetryCount        失败重试次数(大于等于0时使用这个参数，小于零使用任务本身的失败重试次数)
   * @param executorShardingParam 执行器分片参数(‘/’分隔)
   * @param executorParam         执行参数(为空使用任务本身的参数，不为空使用这个参数)
   * @param addressList           机器地址(多地址逗号分隔),为空使用执行器的地址列表（自动获取),不为空使用这个参数 not null: cover
   */
  public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount,
      String executorShardingParam, String executorParam, String addressList) {

    // 加载任务信息
    XxlJobInfo jobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().loadById(jobId);
    if (jobInfo == null) {
      log.warn(">>>>>>>>>>>> trigger fail, jobId invalid，jobId={}", jobId);
      return;
    }
    if (executorParam != null) {
      jobInfo.setExecutorParam(executorParam);
    }
    int finalFailRetryCount =
        failRetryCount >= 0 ? failRetryCount : jobInfo.getExecutorFailRetryCount();
    //获取该类型的执行器信息
    XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao()
        .load(jobInfo.getJobGroup());

    // cover addressList
    //设置地址列表
    if (addressList != null && addressList.trim().length() > 0) {
      group.setAddressType(1);
      group.setAddressList(addressList.trim());
    }

    // sharding param
    //分片信息
    int[] shardingParam = null;
    if (executorShardingParam != null) {
      String[] shardingArr = executorShardingParam.split("/");
      if (shardingArr.length == 2 && isNumeric(shardingArr[0]) && isNumeric(shardingArr[1])) {
        shardingParam = new int[2];
        shardingParam[0] = Integer.valueOf(shardingArr[0]);
        shardingParam[1] = Integer.valueOf(shardingArr[1]);
      }
    }
    // 路由:分片广播
    if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == ExecutorRouteStrategyEnum
        .match(jobInfo.getExecutorRouteStrategy(), null)
        && !CollectionUtils.isEmpty(group.getRegistryList()) && shardingParam == null) {
      for (int i = 0; i < group.getRegistryList().size(); i++) {
        processTrigger(group, jobInfo, finalFailRetryCount, triggerType, i,
            group.getRegistryList().size());
      }
    } else {
      // 路由:非分片广播
      if (shardingParam == null) {
        shardingParam = new int[]{0, 1};
      }
      processTrigger(group, jobInfo, finalFailRetryCount, triggerType, shardingParam[0],
          shardingParam[1]);
    }

  }

  /**
   * 判断String是否为数值，包括小数和负数.
   *
   * @param str 字符串
   * @return 为数值, true.否则，false
   */
  private static boolean isNumeric(String str) {
    return str.matches("^[0-9]+(.[0-9]+)?$");
  }

  /**
   * 执行调度
   *
   * @param group               执行器信息，XxlJobGroup.registryList可能为空
   * @param jobInfo             任务信息
   * @param finalFailRetryCount 失败重试次数失败重试次数
   * @param triggerType         触发类型
   * @param index               分片下标
   * @param total               总分片数
   * @see XxlJobGroup#getRegistryList()
   */
  private static void processTrigger(XxlJobGroup group, XxlJobInfo jobInfo, int finalFailRetryCount,
      TriggerTypeEnum triggerType, int index, int total) {

    // 阻塞策略,默认为单机串行
    ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum
        .match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);
    //路由策略，官方一共10中路由策略
    ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum
        .match(jobInfo.getExecutorRouteStrategy(), null);
    //分片参数，不为分片广播为null(格式为：0/1)
    String shardingParam =
        (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) ? String
            .valueOf(index).concat("/").concat(String.valueOf(total)) : null;

    // 1、save log-id
    XxlJobLog jobLog = new XxlJobLog();
    jobLog.setJobGroup(jobInfo.getJobGroup());
    jobLog.setJobId(jobInfo.getId());
    jobLog.setTriggerTime(new Date());
    XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().save(jobLog);
    log.debug(">>>>>>>>>>> xxl-job trigger start, jobId:{}", jobLog.getId());

    // 2、初始化调度参数
    TriggerParam triggerParam = new TriggerParam();
    triggerParam.setJobId(jobInfo.getId());
    triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
    triggerParam.setExecutorParams(jobInfo.getExecutorParam());
    triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
    triggerParam.setExecutorTimeout(jobInfo.getExecutorTimeout());
    triggerParam.setLogId(jobLog.getId());
    triggerParam.setLogDateTime(jobLog.getTriggerTime().getTime());
    triggerParam.setGlueType(jobInfo.getGlueType());
    triggerParam.setGlueSource(jobInfo.getGlueSource());
    triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
    triggerParam.setBroadcastIndex(index);
    triggerParam.setBroadcastTotal(total);

    // 3、初始化地址
    String address = null;
    ReturnT<String> routeAddressResult = null;
    if (!CollectionUtils.isEmpty(group.getRegistryList())) {
      if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum) {
        if (index < group.getRegistryList().size()) {
          address = group.getRegistryList().get(index);
        } else {
          address = group.getRegistryList().get(0);
        }
      } else {
        routeAddressResult = executorRouteStrategyEnum.getRouter()
            .route(triggerParam, group.getRegistryList());
        if (routeAddressResult.getCode() == ReturnT.SUCCESS_CODE) {
          address = routeAddressResult.getContent();
        }
      }
    } else {
      routeAddressResult = new ReturnT<>(ReturnT.FAIL_CODE,
          I18nUtil.getString("jobconf_trigger_address_empty"));
    }

    // trigger remote executor
    // 4、调度远程执行器
    ReturnT<String> triggerResult = null;
    if (address != null) {
      triggerResult = runExecutor(triggerParam, address);
    } else {
      triggerResult = new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

    // 5、收集调度信息
    StringBuffer triggerMsgSb = new StringBuffer();
    triggerMsgSb.append(I18nUtil.getString("jobconf_trigger_type")).append("：")
        .append(triggerType.getTitle());
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_admin_adress"))
        .append("：").append(IpUtil.getIp());
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regtype"))
        .append("：")
        .append((group.getAddressType() == 0) ? I18nUtil.getString("jobgroup_field_addressType_0")
            : I18nUtil.getString("jobgroup_field_addressType_1"));
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobconf_trigger_exe_regaddress"))
        .append("：").append(group.getRegistryList());
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorRouteStrategy"))
        .append("：").append(executorRouteStrategyEnum.getTitle());
    if (shardingParam != null) {
      triggerMsgSb.append("(" + shardingParam + ")");
    }
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorBlockStrategy"))
        .append("：").append(blockStrategy.getTitle());
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_timeout")).append("：")
        .append(jobInfo.getExecutorTimeout());
    triggerMsgSb.append("<br>").append(I18nUtil.getString("jobinfo_field_executorFailRetryCount"))
        .append("：").append(finalFailRetryCount);

    triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil
        .getString("jobconf_trigger_run") + "<<<<<<<<<<< </span><br>")
        .append((routeAddressResult != null && routeAddressResult.getMsg() != null) ?
            routeAddressResult.getMsg() + "<br><br>" : "")
        .append(triggerResult.getMsg() != null ? triggerResult.getMsg() : "");

    // 6、保存日志调度信息
    jobLog.setExecutorAddress(address);
    jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
    jobLog.setExecutorParam(jobInfo.getExecutorParam());
    jobLog.setExecutorShardingParam(shardingParam);
    jobLog.setExecutorFailRetryCount(finalFailRetryCount);
    jobLog.setTriggerCode(triggerResult.getCode());
    jobLog.setTriggerMsg(triggerMsgSb.toString());
    XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateTriggerInfo(jobLog);

    log.debug(">>>>>>>>>>> xxl-job trigger end, jobId:{}", jobLog.getId());
  }

  /**
   * api调度执行器.
   *
   * @param triggerParam 调度参数
   * @param address      远程地址
   * @return 调度的结果
   */
  public static ReturnT<String> runExecutor(TriggerParam triggerParam, String address) {
    ReturnT<String> runResult = null;
    try {
      ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
      runResult = executorBiz.run(triggerParam);
    } catch (Exception e) {
      log.error(">>>>>>>>>>> xxl-job trigger error, please check if the executor[{}] is running.",
          address, e);
      runResult = new ReturnT<>(ReturnT.FAIL_CODE, ThrowableUtil.toString(e));
    }

    StringBuffer runResultSB = new StringBuffer(I18nUtil.getString("jobconf_trigger_run") + "：");
    runResultSB.append("<br>address：").append(address);
    runResultSB.append("<br>code：").append(runResult.getCode());
    runResultSB.append("<br>msg：").append(runResult.getMsg());

    runResult.setMsg(runResultSB.toString());
    return runResult;
  }
}
