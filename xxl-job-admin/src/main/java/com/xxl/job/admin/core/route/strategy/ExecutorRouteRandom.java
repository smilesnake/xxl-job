package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

/**
 * （随机）：随机选择在线的机器；
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteRandom extends ExecutorRouter {

    private static Random localRandom = new SecureRandom();

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        // 在地址列表的大小内随机找出一个地址l.
        String address = addressList.get(localRandom.nextInt(addressList.size()));
        return new ReturnT<>(address);
    }

}
