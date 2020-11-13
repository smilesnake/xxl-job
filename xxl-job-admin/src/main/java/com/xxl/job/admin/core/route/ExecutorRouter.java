package com.xxl.job.admin.core.route;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.util.List;

/**
 * 路由执行器.
 *
 * @author xuxueli on 17/3/10.
 */
public abstract class ExecutorRouter {
    /**
     * 路由地址
     *
     * @param triggerParam 调度参数
     * @param addressList  地址列表
     * @return ReturnT.content=address,返回为路由后的地址
     */
    public abstract ReturnT<String> route(TriggerParam triggerParam, List<String> addressList);

}
