package com.xxl.job.core.handler.impl;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 方法任务处理器.
 *
 * @author xuxueli 2019-12-11 21:12:18
 */
@AllArgsConstructor
public class MethodJobHandler extends IJobHandler {
    /**
     * 目标类
     */
    private final Object target;
    /**
     * 目标方法
     */
    private final Method method;
    /**
     * 目标初始化方法
     */
    private Method initMethod;
    /**
     * 目标销毁方法
     */
    private Method destroyMethod;

    @Override
    public ReturnT<String> execute(String param) throws InvocationTargetException, IllegalAccessException {
        return (ReturnT<String>) method.invoke(target, new Object[]{param});
    }

    @Override
    public void init() throws InvocationTargetException, IllegalAccessException {
        if (initMethod != null) {
            initMethod.invoke(target);
        }
    }

    @Override
    public void destroy() throws InvocationTargetException, IllegalAccessException {
        if (destroyMethod != null) {
            destroyMethod.invoke(target);
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[" + target.getClass() + "#" + method.getName() + "]";
    }
}
