package com.xxl.job.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 阻塞策略枚举
 *
 * @author xuxueli on 17/5/9.
 */
@AllArgsConstructor
@Getter
public enum ExecutorBlockStrategyEnum {
    /**
     * 单机串行.
     */
    SERIAL_EXECUTION("Serial execution"),
    /**
     * 丢弃后续调度
     */
    /*CONCURRENT_EXECUTION("并行"),*/
    DISCARD_LATER("Discard Later"),
    /**
     * 覆盖之前调度
     */
    COVER_EARLY("Cover Early");
    /**
     * 标题.
     */
    @Setter
    private String title;

    public static ExecutorBlockStrategyEnum match(String name, ExecutorBlockStrategyEnum defaultItem) {
        if (name != null) {
            for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
                if (item.name().equals(name)) {
                    return item;
                }
            }
        }
        return defaultItem;
    }
}
