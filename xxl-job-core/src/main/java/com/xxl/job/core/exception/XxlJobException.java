package com.xxl.job.core.exception;

import lombok.NoArgsConstructor;

/**
 * 任务异常.
 *
 * @author xuxueli 2019-05-04 23:19:29
 */
@NoArgsConstructor
public class XxlJobException extends RuntimeException {

    public XxlJobException(String message) {
        super(message);
    }
}
