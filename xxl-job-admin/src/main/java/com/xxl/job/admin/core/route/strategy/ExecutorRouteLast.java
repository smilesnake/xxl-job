package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.util.List;

/**
 * （最后一个）：固定选择最后一个机器
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteLast extends ExecutorRouter {

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        // 直接取得是list最后一个数据
        return new ReturnT<>(addressList.get(addressList.size() - 1));
    }

}
