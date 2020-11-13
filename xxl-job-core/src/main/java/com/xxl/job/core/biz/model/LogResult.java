package com.xxl.job.core.biz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 日志结果.
 *
 * @author xuxueli on 17/3/23.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class LogResult implements Serializable {
    private static final long serialVersionUID = 42L;

    /**
     * 开始的行号.
     */
    private int fromLineNum;
    /**
     * 到达的行号.
     */
    private int toLineNum;
    /**
     * 日志的内容.
     */
    private String logContent;
    /**
     * 是否结束.
     */
    private boolean isEnd;
}
