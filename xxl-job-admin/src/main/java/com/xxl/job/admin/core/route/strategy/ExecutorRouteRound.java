package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteRound extends ExecutorRouter {
    /**
     * 每个任务路由次数缓存.<任务id,路由次数>
     */
    private static ConcurrentMap<Integer, AtomicInteger> routeCountEachJob = new ConcurrentHashMap<>();
    /**
     * 缓存过期时间戳
     */
    private static long CACHE_VALID_TIME = 0;
    /**
     * 临界次数.
     */
    private static final int CRITICAL_COUNT = 1000000;

    /**
     * 统计指定任务路由的次数
     *
     * @param jobId 任务id
     * @return 指定任务路由的次数
     */
    private static int count(int jobId) {
        // 如果当前的时间，大于缓存的时间，那么说明需要刷新了
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            routeCountEachJob.clear();
            // 设置缓存时间戳，默认缓存一天，一天之后会从新开始
            CACHE_VALID_TIME = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        }

        AtomicInteger count = routeCountEachJob.get(jobId);
        // 当第一次执行轮循这个策略的时候，routeCountEachJob这个Map里面肯定是没有这个地址的， count==null ,
        // 当 count==null或者count大于100万的时候，系统会默认在100之间随机一个数字,放入hashMap, 然后返回该数字
        // 当系统第二次进来的时候，count!=null 并且小于100万， 那么把count加1 之后返回出去。
        if (count == null || count.get() > CRITICAL_COUNT) {
            // 初始化时主动Random一次，缓解首次压力
            count = new AtomicInteger(new SecureRandom().nextInt(100));
        } else {
            // count++
            count.getAndIncrement();
        }
        routeCountEachJob.put(jobId, count);
        return count.get();
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        // 在执行器地址列表，获取相应的地址,通过count(jobid) 这个方法来实现，主要逻辑在这个方法
        // 通过count（jobId）拿到数字之后， 通过求余的方式，拿到执行器地址
        // 例： count=2 , addresslist.size = 3
        // 2%3 = 2 ,  则拿list中下表为2的地址
        String address = addressList.get(count(triggerParam.getJobId()) % addressList.size());
        return new ReturnT<>(address);
    }
}