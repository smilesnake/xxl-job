package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import com.xxl.job.admin.core.trigger.XxlJobTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务调度线程池帮助类.
 *
 * @author xuxueli 2018-07-03 21:08:07
 */
@Slf4j
public class JobTriggerPoolHelper {

    // ---------------------- trigger pool ----------------------
    // 快/慢线程池
    /**
     * 快线程池
     */
    private ThreadPoolExecutor fastTriggerPool = null;
    /**
     * 慢线程池，（1分钟内超过500毫秒的请求大于10次）
     */
    private ThreadPoolExecutor slowTriggerPool = null;

    /**
     * 准备线程池.
     */
    public void start() {
        //快线程池默认10个核心线程，最大线程200，任务队列1000
        fastTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> new Thread(r, "xxl-job, admin JobTriggerPoolHelper-fastTriggerPool-" + r.hashCode()));
        //慢线程池默认10个核心线程，最大线程100，任务队列2000
        slowTriggerPool = new ThreadPoolExecutor(
                10,
                XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax(),
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                r -> new Thread(r, "xxl-job, admin JobTriggerPoolHelper-slowTriggerPool-" + r.hashCode()));
    }

    /**
     * 停止运行线程池.
     */
    public void stop() {
        fastTriggerPool.shutdownNow();
        slowTriggerPool.shutdownNow();
        log.info(">>>>>>>>> xxl-job trigger thread pool shutdown success.");
    }


    // 任务超时数量
    /**
     * 最小时间，1分钟.
     */
    private volatile long minTim = System.currentTimeMillis() / 60000;
    /**
     * 任务超时次数<jobId,次数>.
     */
    private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

    /**
     * 添加调度器.
     *
     * @param jobId                 任务id
     * @param triggerType           任务触发类型枚举
     * @param failRetryCount        失败重试次数(大于等于0时使用这个参数，小于零使用任务本身的失败重试次数)
     * @param executorShardingParam 执行器分片参数
     * @param executorParam         执行参数   null: use job param
     *                              not null: cover job param
     * @param addressList           机器地址,为空自动获取
     */
    public void addTrigger(final int jobId,
                           final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {

        // 选择线程池
        //默认使用快线程池
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        //但1分钟内超过500毫秒的请求大于10次，放入慢线程池处理
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {
            triggerPool_ = slowTriggerPool;
        }

        // 调度
        triggerPool_.execute(() -> {
            long start = System.currentTimeMillis();
            try {
                // do trigger
                XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            } finally {

                // check timeout-count-map
                long minTim_now = System.currentTimeMillis() / 60000;
                if (minTim != minTim_now) {
                    minTim = minTim_now;
                    jobTimeoutCountMap.clear();
                }

                // incr timeout-count-map
                long cost = System.currentTimeMillis() - start;
                if (cost > 500) {       // ob-timeout threshold 500ms
                    AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                    if (timeoutCount != null) {
                        timeoutCount.incrementAndGet();
                    }
                }

            }

        });
    }


    // ---------------------- helper ----------------------
    private static JobTriggerPoolHelper helper = new JobTriggerPoolHelper();

    /**
     * 准备线程池
     */
    public static void toStart() {
        helper.start();
    }

    /**
     * 停止运行线程池
     */
    public static void toStop() {
        helper.stop();
    }

    /**
     * 调度任务.
     *
     * @param jobId                 任务id
     * @param triggerType           任务触发类型枚举
     * @param failRetryCount        cc
     * @param executorShardingParam 执行器分片参数(‘/’分隔)
     * @param executorParam         执行参数(为空使用任务本身的参数，不为空使用这个参数)
     * @param addressList           机器地址(多地址逗号分隔),为空使用执行器的地址列表（自动获取),不为空使用这个参数
     */
    public static void trigger(int jobId, TriggerTypeEnum triggerType, int failRetryCount, String executorShardingParam, String executorParam, String addressList) {
        helper.addTrigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
    }

}
