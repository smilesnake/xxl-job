package com.xxl.job.admin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 日志清理类型
 *
 * @author smilesnake
 */
@Getter
@AllArgsConstructor
public enum LogClearType {

    /**
     * 清理一个月之前日志数据
     */
    ONE_MONTH(1, "清理一个月之前日志数据", true, -1),
    /**
     * 清理三个月之前日志数据
     */
    THREE_MONTHS(2, "清理三个月之前日志数据", true, -3),

    /**
     * 清理六个月之前日志数据
     */
    SIX_MONTHS(3, "清理六个月之前日志数据", true, -6),

    /**
     * 清理一年之前日志数据
     */
    ONE_YEAR(4, "清理一年之前日志数据", true, -12),

    /**
     * 清理一千条以前日志数据
     */
    ONE_THOUSAND(5, "清理一千条以前日志数据", false, 1000),

    /**
     * 清理一万条以前日志数据
     */
    TEN_THOUSAND(6, "清理一万条以前日志数据", false, 10000),

    /**
     * 清理三万条以前日志数据
     */
    THIRTY_THOUSAND(7, "清理三万条以前日志数据", false, 30000),

    /**
     * 清理十万条以前日志数据
     */
    HUNDRED_THOUSAND(8, "清理十万条以前日志数据", false, 100000),
    /**
     * 清理所有日志
     */
    ALL(9, "清理所有日志", false, 0);

    /**
     * 标识
     */
    private int key;
    /**
     * 描述.
     */
    private String desc;
    /**
     * 是否为日期.
     */
    private boolean isDate;
    /**
     * 数值.
     */
    private Integer number;

    public static LogClearType match(int key) {
        for (LogClearType item : LogClearType.values()) {
            if (item.key == key) {
                return item;
            }
        }
        return null;
    }
}

