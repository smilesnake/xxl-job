package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度类型枚举.
 *
 * @author xuxueli 2020-10-29 21:11:23
 */
@Getter
@AllArgsConstructor
public enum ScheduleTypeEnum {
  /**
   * 无
   */
  NONE(I18nUtil.getString("schedule_type_none")),

  /**
   * cron
   */
  CRON(I18nUtil.getString("schedule_type_cron")),

  /**
   * 固定速度
   */
  FIX_RATE(I18nUtil.getString("schedule_type_fix_rate"));

  /**
   * 标题
   */
  private String title;

  public static ScheduleTypeEnum match(String name, ScheduleTypeEnum defaultItem) {
    for (ScheduleTypeEnum item : ScheduleTypeEnum.values()) {
      if (item.name().equals(name)) {
        return item;
      }
    }
    return defaultItem;
  }

}
