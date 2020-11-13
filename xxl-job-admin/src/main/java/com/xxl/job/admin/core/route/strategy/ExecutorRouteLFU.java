package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * （最不经常使用）：使用频率最低的机器优先被选举
 * <p>单个JOB对应的每个执行器，使用频率最低的优先被选举</p>
 * <ol>
 *     <li> LFU(Least Frequently Used)：最不经常使用，频率/次数</li>
 *     <li> LRU(Least Recently Used)：最近最久未使用，时间</li>
 * </ol>
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteLFU extends ExecutorRouter {

    /**
     * 存储任务ID对应的执行信息<任务id,<执行器地址,执行次数>>
     */
    private static ConcurrentMap<Integer, HashMap<String, AtomicInteger>> jobLfuMap = new ConcurrentHashMap<>();
    /**
     * 缓存过期时间戳
     */
    private long cacheValidTime = 0;
    /**
     * 临界次数.
     */
    private static final int CRITICAL_COUNT = 1000000;

    /**
     * 路由.
     *
     * @param jobId       任务id
     * @param addressList 地址列表
     * @return 最不经常使用的地址
     */
    public String route(int jobId, List<String> addressList) {
        // 如果当前系统时间大于过期时间
        if (System.currentTimeMillis() > cacheValidTime) {
            jobLfuMap.clear();
            // 重新设置过期时间，默认为一天
            cacheValidTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        }

        // lfu item init
        //lfuItemMap中放的是执行器地址以及执行次数
        // Key排序可以用TreeMap+构造入参Compare；Value排序暂时只能通过ArrayList；
        HashMap<String, AtomicInteger> lfuItemMap = jobLfuMap.get(jobId);
        if (lfuItemMap == null) {
            lfuItemMap = new HashMap<>();
            // 避免重复覆盖,如果传入jobId对应的lfuItemMap已经存在，就返回存在的lfuItemMap，不进行替换。如果不存在，就添加jobId和lfuItemMap，
            jobLfuMap.putIfAbsent(jobId, lfuItemMap);
        }

        // put new
        for (String address : addressList) {
            // map中不包含，并且值大于一万的时候，需要重新初始化执行器地址对应的执行次数
            // 初始化的规则是在机器地址列表size里面进行随机
            // 当运行一段时间后，有新机器加入的时候，此时，新机器初始化的执行次数较小，所以一开始，新机器的压力会比较大，后期慢慢趋于平衡
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address).get() > CRITICAL_COUNT) {
                // 初始化时主动Random一次，缓解首次压力
                lfuItemMap.put(address, new AtomicInteger(new Random().nextInt(addressList.size())));
            }
        }

        // 移除存在地址的执行信息
        List<String> delKeys = lfuItemMap.keySet().stream().filter(existKey -> !addressList.contains(existKey)).collect(Collectors.toList());
        if (!delKeys.isEmpty()) {
            for (String delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }

        // 将lfuItemMap中的key.value, 取出来，然后使用Comparator进行排序，value小的靠前。
        List<Map.Entry<String, AtomicInteger>> lfuItemList = new ArrayList<>(lfuItemMap.entrySet());
        Collections.sort(lfuItemList, Comparator.comparing(it -> it.getValue().get()));
        //取第一个，也就是最小的一个，将address返回，同时对该address对应的值加1 。
        Map.Entry<String, AtomicInteger> addressItem = lfuItemList.get(0);
        addressItem.getValue().incrementAndGet();
        return addressItem.getKey();
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = route(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }

}
