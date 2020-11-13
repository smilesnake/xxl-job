package com.xxl.job.core.handler;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.thread.JobThread;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * 任务处理器.
 *
 * @author xuxueli 2015-12-19 19:06:38
 */
public abstract class IJobHandler {


    /**
     * 成功
     */
    public static final ReturnT<String> SUCCESS = new ReturnT<>(200, null);
    /**
     * 失败
     */
    public static final ReturnT<String> FAIL = new ReturnT<>(500, null);
    /**
     * 超时
     */
    public static final ReturnT<String> FAIL_TIMEOUT = new ReturnT<>(502, null);


    /**
     * 执行处理器，当executor收到调度请求时调用
     *
     * @param param 用于方法调用的参数
     * @return 执行结果
     * @throws InvocationTargetException 反射异常
     * @throws IllegalAccessException    没有访问权限，抛出
     * @throws IOException               写读异常
     * @throws InterruptedException      中断，抛出
     */
    public abstract ReturnT<String> execute(String param) throws InvocationTargetException, IllegalAccessException, IOException, InterruptedException;


    /**
     * 初始化处理程序，在JobThread初始化时调用
     *
     * @throws InvocationTargetException 反射异常
     * @throws IllegalAccessException    没有访问权限，抛出
     * @see JobThread
     */
    public void init() throws InvocationTargetException, IllegalAccessException {
        // do something
    }


    /**
     * 销毁处理程序，在JobThread销毁时调用
     *
     * @throws InvocationTargetException 反射异常
     * @throws IllegalAccessException    没有访问权限，抛出
     * @see JobThread
     */
    public void destroy() throws InvocationTargetException, IllegalAccessException {
        // do something
    }


}
