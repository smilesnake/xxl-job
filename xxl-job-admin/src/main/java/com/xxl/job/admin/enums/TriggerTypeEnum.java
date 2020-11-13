package com.xxl.job.admin.enums;

import com.xxl.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务触发类型枚举.
 *
 * @author xuxueli 2018-09-16 04:56:41
 */
@Getter
@AllArgsConstructor
public enum TriggerTypeEnum {
  /**
   * 手动触发.
   */
  MANUAL(I18nUtil.getString("jobconf_trigger_type_manual")),
  /**
   * Cron触发.
   */
  CRON(I18nUtil.getString("jobconf_trigger_type_cron")),
  /**
   * 失败重试触发.
   */
  RETRY(I18nUtil.getString("jobconf_trigger_type_retry")),
  /**
   * 父任务触发.
   */
  PARENT(I18nUtil.getString("jobconf_trigger_type_parent")),
  /**
   * API触发.
   */
  API(I18nUtil.getString("jobconf_trigger_type_api")),

  /**
   * 调度过期补偿調
   */
  MISFIRE(I18nUtil.getString("jobconf_trigger_type_misfire"));
  /**
   * 标题.
   */
  private String title;
}