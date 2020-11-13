package com.xxl.job.core.handler.impl;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.log.XxlJobLogger;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * glue任务处理器
 *
 * @author xuxueli 2016-5-19 21:05:45
 */
@AllArgsConstructor
public class GlueJobHandler extends IJobHandler {
    /**
     * 任务处理器
     */
    private IJobHandler jobHandler;
    /**
     * GLUE更新时间
     */
    @Getter
    private long glueUpdateTime;

    @Override
    public ReturnT<String> execute(String param) throws InterruptedException, IllegalAccessException, IOException, InvocationTargetException {
        XxlJobLogger.log("----------- glue.version:" + glueUpdateTime + " -----------");
        return jobHandler.execute(param);
    }
}