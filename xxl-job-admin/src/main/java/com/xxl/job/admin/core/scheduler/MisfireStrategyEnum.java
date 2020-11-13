package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.util.I18nUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 调度过度策略枚举
 *
 * @author xuxueli 2020-10-29 21:11:23
 */
@AllArgsConstructor
@Getter
public enum MisfireStrategyEnum {

  /**
   * 忽略
   */
  DO_NOTHING(I18nUtil.getString("misfire_strategy_do_nothing")),

  /**
   * 立即执行一次
   */
  FIRE_ONCE_NOW(I18nUtil.getString("misfire_strategy_fire_once_now"));

  /**
   * 标题
   */
  private String title;

  public static MisfireStrategyEnum match(String name, MisfireStrategyEnum defaultItem) {
    for (MisfireStrategyEnum item : MisfireStrategyEnum.values()) {
      if (item.name().equals(name)) {
        return item;
      }
    }
    return defaultItem;
  }

}
