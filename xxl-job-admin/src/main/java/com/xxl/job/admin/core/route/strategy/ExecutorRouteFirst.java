package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.util.List;

/**
 * （第一个）固定选择第一个机器
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteFirst extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        // 直接取集群地址列表里面的第一台机器来进行执行
        return new ReturnT<>(addressList.get(0));
    }

}
