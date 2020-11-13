package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * （最近最久未使用）：最久未使用的机器优先被选举
 * <p>单个JOB对应的每个执行器，最久为使用的优先被选举</p>
 * <ol>
 *     <li> LFU(Least Frequently Used)：最不经常使用，频率/次数</li>
 *     <li> LRU(Least Recently Used)：最近最久未使用，时间</li>
 * </ol>
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteLRU extends ExecutorRouter {
    /**
     * 存储任务ID对应的执行信息<任务id,<执行器地址,执行器地址>>
     */
    private static ConcurrentMap<Integer, LinkedHashMap<String, String>> jobLRUMap = new ConcurrentHashMap<>();
    /**
     * 过期时间戳
     */
    private long cacheValidTime = 0;

    /**
     * 路由.
     *
     * @param jobId       任务id
     * @param addressList 地址列表
     * @return 最近最久未使用的地址
     */
    private String route(int jobId, List<String> addressList) {

        // cache clear
        // 如果当前系统时间大于过期时间
        if (System.currentTimeMillis() > cacheValidTime) {
            jobLRUMap.clear();
            // 重新设置过期时间，默认为一天
            cacheValidTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        }

        // init lru
        LinkedHashMap<String, String> lruItem = jobLRUMap.get(jobId);
        if (lruItem == null) {
            /**
             * LinkedHashMap
             *      a、accessOrder：true=访问顺序排序（get/put时排序）；false=插入顺序排期；
             *      b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；可封装LinkedHashMap并重写该方法，比如定义最大容量，超出是返回true即可实现固定长度的LRU算法；
             */
            lruItem = new LinkedHashMap<>(16, 0.75f, true);
            jobLRUMap.putIfAbsent(jobId, lruItem);
        }

        // 如果地址列表里面有地址不在map中，此处是可以再次放入，防止添加机器的问题
        for (String address : addressList) {
            if (!lruItem.containsKey(address)) {
                lruItem.put(address, address);
            }
        }
        // 移除旧的地址
        List<String> delKeys = new ArrayList<>();
        for (String existKey : lruItem.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (!delKeys.isEmpty()) {
            for (String delKey : delKeys) {
                lruItem.remove(delKey);
            }
        }

        // 取头部的一个元素，也就是最久操作过的数据
        String eldestKey = lruItem.entrySet().iterator().next().getKey();
        return lruItem.get(eldestKey);
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }

}
