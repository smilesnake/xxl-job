package com.xxl.job.admin.core.complete;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import java.text.MessageFormat;
import lombok.extern.slf4j.Slf4j;

/**
 * XXl - Job 完成器，即执行最后的任务
 *
 * @author xuxueli 2020-10-30 20:43:10
 */
@Slf4j
public class XxlJobCompleter {


  /**
   * 组件刷新处理入口 (仅限一次)
   *
   * @param xxlJobLog 任务信息
   * @return 更新影响的行数
   */
  public static int updateHandleInfoAndFinish(XxlJobLog xxlJobLog) {

    // 结束任务
    finishJob(xxlJobLog);

    // text最大64kb 避免长度过长
    if (xxlJobLog.getHandleMsg().length() > 15000) {
      xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg().substring(0, 15000));
    }

    // 刷新处理信息
    return XxlJobAdminConfig.getAdminConfig().getXxlJobLogDao().updateHandleInfo(xxlJobLog);
  }


  /**
   * 做一些事来结束任务
   *
   * @param xxlJobLog 任务日志
   */
  private static void finishJob(XxlJobLog xxlJobLog) {

    // 1、处理成功，调度子任务
    String triggerChildMsg = null;
    if (XxlJobContext.HANDLE_COCE_SUCCESS == xxlJobLog.getHandleCode()) {
      // 加载任务信息
      XxlJobInfo xxlJobInfo = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao()
          .loadById(xxlJobLog.getJobId());
      if (xxlJobInfo != null && xxlJobInfo.getChildJobId() != null
          && xxlJobInfo.getChildJobId().trim().length() > 0) {
        triggerChildMsg = "<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>" + I18nUtil
            .getString("jobconf_trigger_child_run") + "<<<<<<<<<<< </span><br>";

        // 子任务id列表
        String[] childJobIds = xxlJobInfo.getChildJobId().split(",");
        for (int i = 0; i < childJobIds.length; i++) {
          // 子任务id
          int childJobId = (childJobIds[i] != null && childJobIds[i].trim().length() > 0
              && isNumeric(childJobIds[i])) ? Integer.valueOf(childJobIds[i]) : -1;
          if (childJobId > 0) {

            // 调度任务
            JobTriggerPoolHelper.trigger(childJobId, TriggerTypeEnum.PARENT, -1, null, null, null);
            ReturnT<String> triggerChildResult = ReturnT.SUCCESS;

            // 调度子任务消息
            triggerChildMsg += MessageFormat
                .format(I18nUtil.getString("jobconf_callback_child_msg1"),
                    (i + 1), childJobIds.length, childJobIds[i], (triggerChildResult.getCode()
                        == ReturnT.SUCCESS_CODE ? I18nUtil.getString("system_success")
                        : I18nUtil.getString("system_fail")), triggerChildResult.getMsg());
          } else {
            triggerChildMsg += MessageFormat
                .format(I18nUtil.getString("jobconf_callback_child_msg2"), (i + 1),
                    childJobIds.length, childJobIds[i]);
          }
        }

      }
    }

    // 设置处理消息
    if (triggerChildMsg != null) {
      xxlJobLog.setHandleMsg(xxlJobLog.getHandleMsg() + triggerChildMsg);
    }

    // 2、fix_delay trigger next
    // on the way

  }

  /**
   * 判断字符串是否为数字
   *
   * @param str 指定的字符串
   * @return true, 是数字，否则，false
   */
  private static boolean isNumeric(String str) {
    try {
      int result = Integer.valueOf(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

}
