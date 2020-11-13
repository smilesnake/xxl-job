package com.xxl.job.admin.core.scheduler;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.thread.*;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.client.ExecutorBizClient;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务调度
 *
 * @author xuxueli 2018-10-28 00:18:17
 */

public class XxlJobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(XxlJobScheduler.class);

    /**
     * 初始化.
     */
    public void init() {
        // 国际化配置初始化
        initI18n();

        // admin registry monitor run
        // 监听执行器在线状态
        // 1.调度任务注册监控助手运行,每30s运行一次,主要监听90秒之内没有更新信息的注册机器删除掉
        // 2.查询90s以内有更新的机器列表,并且把这些机器的最新ip更新到XxlJobGroup表,多个地址以逗号分隔
        JobRegistryMonitorHelper.getInstance().start();

        // 开始监听失败任务 -> 重试
        JobFailMonitorHelper.getInstance().start();

        //  admin lose-monitor run
        // 开始监听丢失任务（任务结果丢失处理：调度记录停留在 "运行中" 状态超过10min，且对应执行器心跳注册失败不在线，则将本地调度主动标记失败；）
        JobLoseMonitorHelper.getInstance().start();

        // 初始化调度线程池
        JobTriggerPoolHelper.toStart();

        // admin log report start
        // 任务日志统计刷新
        JobLogReportHelper.getInstance().start();

        // 启动定时任务调度器（执行任务，缓存任务）
        JobScheduleHelper.getInstance().start();

        logger.info(">>>>>>>>> init xxl-job admin success.");
    }


    public void destroy() {

        // stop-schedule
        // 停止监听执行器在线状态
        JobScheduleHelper.getInstance().toStop();

        // admin log report stop
        // 停止
        JobLogReportHelper.getInstance().toStop();

        //停止运行触发-执行器线程池
        JobTriggerPoolHelper.toStop();

        // admin lose-monitor stop
        // 停止监听丢失任务
        JobLoseMonitorHelper.getInstance().toStop();

        // admin fail-monitor stop
        // 停止监听失败任务
        JobFailMonitorHelper.getInstance().toStop();

        // admin registry stop
        // 停止监听执行器在线状态
        JobRegistryMonitorHelper.getInstance().toStop();

    }

    // ---------------------- I18n ----------------------

    /**
     * 初始化I18n
     */
    private void initI18n() {
        //将枚举title替换为国际化配置
        for (ExecutorBlockStrategyEnum item : ExecutorBlockStrategyEnum.values()) {
            item.setTitle(I18nUtil.getString("jobconf_block_".concat(item.name())));
        }
    }

    // ---------------------- 执行器客户端 ----------------------
    /**
     * 业务执行器存储库<address,ExecutorBiz>.
     */
    private static ConcurrentMap<String, ExecutorBiz> executorBizRepository = new ConcurrentHashMap<>();

    /**
     * 得到执行器.
     *
     * @param address 地址
     * @return api调度执行器
     */
    public static ExecutorBiz getExecutorBiz(String address) {
        // 验证
        if (StringUtils.isBlank(address)) {
            return null;
        }

        // 加载缓存,从内存中获取执行器对象
        address = address.trim();
        ExecutorBiz executorBiz = executorBizRepository.get(address);
        if (executorBiz != null) {
            return executorBiz;
        }

        // 设置缓存
        executorBiz = new ExecutorBizClient(address, XxlJobAdminConfig.getAdminConfig().getAccessToken());

        executorBizRepository.put(address, executorBiz);
        return executorBiz;
    }

}
