package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.concurrent.TimeUnit;

/**
 * 注册执行器线程.
 *
 * @author xuxueli on 17/3/2.
 */
@Slf4j
public class ExecutorRegistryThread {

    private static ExecutorRegistryThread instance = new ExecutorRegistryThread();

    public static ExecutorRegistryThread getInstance() {
        return instance;
    }

    /**
     * 注册线程.
     */
    private Thread registryThread;
    /**
     * 安全停止.
     */
    private volatile boolean toStop = false;

    /**
     * 启动注册
     *
     * @param appName 应用名称
     * @param address 地址（ip+port）
     */
    public void start(final String appName, final String address) {

        // 验证
        if (StringUtils.isBlank(appName)) {
            log.warn(">>>>>>>>>>> xxl-job, executor registry config fail, appName is null.");
            return;
        }
        if (CollectionUtils.isEmpty(XxlJobExecutor.getAdminBizList())) {
            log.warn(">>>>>>>>>>> xxl-job, executor registry config fail, adminAddresses is null.");
            return;
        }
        // 此线程为守护线程，不销毁，循环执行
        registryThread = new Thread(() -> {

            // registry
            while (!toStop) {

                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName, address);
                for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                    try {
                        ReturnT<String> registryResult = adminBiz.registry(registryParam);
                        //注册成功
                        if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                            registryResult = ReturnT.SUCCESS;
                            log.debug(">>>>>>>>>>> xxl-job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            break;
                        } else {
                            //注册失败
                            log.info(">>>>>>>>>>> xxl-job registry fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        }
                    } catch (Exception e) {
                        // 注册时发生了异常
                        log.info(">>>>>>>>>>> xxl-job registry error, registryParam:{}", registryParam, e);
                    }
                }

                try {
                    if (!toStop) {
                        //30秒注册一次
                        TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                    }
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.warn(">>>>>>>>>>> xxl-job, executor registry thread interrupted, error msg:{}", e.getMessage());
                        Thread.currentThread().interrupt();
                    }
                }
            }


            // registry remove 服务中心移除此任务起效果
            //一旦停止注册，那么就会移除任务

            RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistryType.EXECUTOR.name(), appName, address);
            for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
                try {
                    ReturnT<String> registryResult = adminBiz.registryRemove(registryParam);
                    if (registryResult != null && ReturnT.SUCCESS_CODE == registryResult.getCode()) {
                        registryResult = ReturnT.SUCCESS;
                        log.info(">>>>>>>>>>> xxl-job registry-remove success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        break;
                    } else {
                        log.info(">>>>>>>>>>> xxl-job registry-remove fail, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        log.info(">>>>>>>>>>> xxl-job registry-remove error, registryParam:{}", registryParam, e);
                        Thread.currentThread().interrupt();
                    }

                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor registry thread destory.");
        });
        registryThread.setDaemon(true);
        registryThread.setName("xxl-job, executor ExecutorRegistryThread");
        registryThread.start();
    }

    /**
     * 停止注册
     */
    public void toStop() {
        toStop = true;
        // interrupt and wait
        registryThread.interrupt();
        try {
            registryThread.join();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

}
