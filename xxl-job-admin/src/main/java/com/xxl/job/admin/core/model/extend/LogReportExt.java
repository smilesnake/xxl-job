package com.xxl.job.admin.core.model.extend;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.math.NumberUtils;

/**
 * 日志统计扩展类.
 *
 * @author smilesnake
 * @date 2020-09-04
 */
@Setter
public class LogReportExt {
    /**
     * 调度总数量.
     */
    private Integer triggerDayCount = NumberUtils.INTEGER_ZERO;
    /**
     * 调度运行中的数量.
     */
    @Getter
    private Integer triggerDayCountRunning = NumberUtils.INTEGER_ZERO;
    /**
     * 调度成功的数量.
     */
    @Getter
    private Integer triggerDayCountSuc = NumberUtils.INTEGER_ZERO;
    /**
     * 调度失败的数量.
     */
    private Integer triggerDayCountFail = NumberUtils.INTEGER_ZERO;

    public Integer getTriggerDayCountFail() {
        if (triggerDayCountFail.equals(NumberUtils.INTEGER_ZERO)) {
            triggerDayCountFail = triggerDayCount - triggerDayCountRunning - triggerDayCountSuc;
        }
        return triggerDayCountFail;
    }
}
