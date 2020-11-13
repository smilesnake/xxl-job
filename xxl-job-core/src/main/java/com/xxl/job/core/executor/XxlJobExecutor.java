package com.xxl.job.core.executor;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.client.AdminBizClient;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.server.EmbedServer;
import com.xxl.job.core.thread.JobLogFileCleanThread;
import com.xxl.job.core.thread.JobThread;
import com.xxl.job.core.thread.TriggerCallbackThread;
import com.xxl.job.core.util.IpUtil;
import com.xxl.job.core.util.NetUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.net.BindException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 任务执行器
 *
 * @author xuxueli on 2016/3/2 21:14.
 */
@Slf4j
@Setter
public class XxlJobExecutor {
    // ---------------------- param ----------------------
    /**
     * 调度中心部署根地址 [选填]：如调度中心集群部署存在多个地址则用逗号分隔。执行器将会使用该地址进行"执行器心跳注册"和"任务结果回调"；为空则关闭自动注册；
     */
    private String adminAddresses;
    /**
     * 执行器通讯TOKEN [选填]：非空时启用；
     */
    private String accessToken;
    /**
     * 执行器AppName [选填]：执行器心跳注册分组依据；为空则关闭自动注册
     */
    private String appname;
    /**
     * 执行器注册地址 [选填]：优先使用该配置作为注册地址，为空时使用内嵌服务 ”IP:PORT“ 作为注册地址。从而更灵活的支持容器类型执行器动态IP和动态映射端口问题。
     */
    private String address;
    /**
     * 执行器IP [选填]：默认为空表示自动获取IP，多网卡时可手动设置指定IP，该IP不会绑定Host仅作为通讯实用；地址信息用于 "执行器注册" 和 "调度中心请求并触发任务"；
     */
    private String ip;
    /**
     * 执行器端口号 [选填]：小于等于0则自动获取；默认端口为9999，单机部署多个执行器时，注意要配置不同执行器端口；
     */
    private int port;
    /**
     * 执行器运行日志文件存储磁盘路径 [选填] ：需要对该路径拥有读写权限；为空则使用默认路径；
     */
    private String logPath;
    /**
     * 执行器日志文件保存天数 [选填] ： 过期日志自动清理, 限制值大于等于3时生效; 否则, 如-1, 关闭自动清理功能
     */
    private int logRetentionDays;


    // ---------------------- start + stop ----------------------

    /**
     * 启动任务执行器
     *
     * @throws BindException 端口绑定失败，抛出
     */
    public void start() throws BindException {

        // 初始化log日志
        XxlJobFileAppender.initLogPath(logPath);

        // 初始化调用调度中心的client列表
        initAdminBizList(adminAddresses, accessToken);

        // 初始化日志文件清理线程
        JobLogFileCleanThread.getInstance().start(logRetentionDays);

        // 初始化触发器回调线程(用RPC回调调度中心接口)
        TriggerCallbackThread.getInstance().start();

        // 初始化执行器服务
        initEmbedServer(address, ip, port, appname, accessToken);
    }

    /**
     * 销毁.
     */
    public void destroy() {
        // 停止内嵌服务
        stopEmbedServer();

        // 销毁任务线程
        if (jobThreadRepository.size() > 0) {
            for (Map.Entry<Integer, JobThread> item : jobThreadRepository.entrySet()) {
                JobThread oldJobThread = removeJobThread(item.getKey(), "web container destroy and kill the job.");
                // wait for job thread push result to callback queue
                //等待任务线程放入结果至回调队列
                if (oldJobThread != null) {
                    try {
                        oldJobThread.join();
                    } catch (InterruptedException e) {
                        log.error(">>>>>>>>>>> xxl-job, JobThread destroy(join) error, jobId:{}", item.getKey(), e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
            jobThreadRepository.clear();
        }
        jobHandlerRepository.clear();

        // destory JobLogFileCleanThread
        // 销毁日志文件清理线程
        JobLogFileCleanThread.getInstance().toStop();

        // destory TriggerCallbackThread
        // 销毁调度回调线程
        TriggerCallbackThread.getInstance().toStop();

    }


    // ---------------------- admin-client (rpc invoker)（管理客户端<rpc调用>） ----------------------
    /**
     * 调度中心部署根地址列表
     */
    @Getter
    private static List<AdminBiz> adminBizList;

    /**
     * 初始化调度中心.
     *
     * @param adminAddresses 调度中心部署根地址
     * @param accessToken    访问令牌
     */
    private void initAdminBizList(String adminAddresses, String accessToken) {
        if (StringUtils.isNotBlank(adminAddresses)) {
            for (String addr : adminAddresses.trim().split(",")) {
                if (StringUtils.isNotBlank(addr)) {
                    AdminBiz adminBiz = new AdminBizClient(addr.trim(), accessToken);
                    if (adminBizList == null) {
                        adminBizList = new ArrayList<>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }

    // ---------------------- executor-server (rpc provider) ----------------------
    /**
     * 内嵌服务器
     */
    private EmbedServer embedServer = null;

    /**
     * 初始化内嵌服务.
     *
     * @param address     执行器注册地址
     * @param ip          执行器IP
     * @param port        执行器端口号
     * @param appName     执行器AppName
     * @param accessToken 执行器通讯TOKEN
     * @throws BindException 端口被占用，抛出
     */
    private void initEmbedServer(String address, String ip, int port, String appName, String accessToken) throws BindException {

        //填充地址
        port = port > 0 ? port : NetUtil.findAvailablePort(9999);
        ip = StringUtils.isNotBlank(ip) ? ip : IpUtil.getIp();

        // 生成地址
        if (StringUtils.isBlank(address)) {
            // 注册地址，默认使用address去注册，如果address为空就使用ip:port去注册
            String ipPortAddress = IpUtil.getIpPort(ip, port);
            address = "http://{ip_port}/".replace("{ip_port}", Objects.requireNonNull(ipPortAddress));
        }

        // start
        embedServer = new EmbedServer();
        embedServer.start(address, port, appName, accessToken);
    }

    /**
     * 停止内嵌服务.
     */
    private void stopEmbedServer() {
        // stop provider factory
        try {
            embedServer.stop();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    // ---------------------- job handler repository ----------------------
    /**
     * 任务处理器存储库<  @XxlJob#value,任务处理器(IJobHandler)>
     *
     * @see XxlJob#value()
     */
    private static ConcurrentMap<String, IJobHandler> jobHandlerRepository = new ConcurrentHashMap<>();

    /**
     * 注册/添加任务处理器.
     *
     * @param name       XxlJob#value()
     * @param jobHandler 任务处理器
     * @see XxlJob#value()
     */
    public static void registryJobHandler(String name, IJobHandler jobHandler) {
        log.info(">>>>>>>>>>> xxl-job register jobhandler success, name:{}, jobHandler:{}", name, jobHandler);
        jobHandlerRepository.put(name, jobHandler);
    }

    /**
     * 加载任务处理器.
     *
     * @param name XxlJob#value()
     * @return 如果存在，返回指定name的任务处理器，如果不存在，返回null
     * @see XxlJob#value()
     */
    public static IJobHandler loadJobHandler(String name) {
        return jobHandlerRepository.get(name);
    }


    // ---------------------- job thread repository ----------------------
    /**
     * 任务线程存储库<jobId, JobThread>.
     */
    private static ConcurrentMap<Integer, JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    /**
     * 注册任务线程
     *
     * @param jobId           任务id
     * @param handler         任务处理器
     * @param removeOldReason 移除老线程的原因
     * @return 新的任务线程
     */
    public static JobThread registerJobThread(int jobId, IJobHandler handler, String removeOldReason) {
        //新的线程跑起来，旧的线程中断，停止
        JobThread newJobThread = new JobThread(jobId, handler);
        newJobThread.start();
        log.info(">>>>>>>>>>> xxl-job regist JobThread success, jobId:{}, handler:{}", new Object[]{jobId, handler});

        //put返回是旧数据
        JobThread oldJobThread = jobThreadRepository.put(jobId, newJobThread);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
        }

        return newJobThread;
    }

    /**
     * 移除旧任务线程.
     *
     * @param jobId           任务id
     * @param removeOldReason 移除旧线程的原因
     * @return 移除的线程，为空表示没找到任务线程id为jobId的线程
     */
    public static JobThread removeJobThread(int jobId, String removeOldReason) {
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if (oldJobThread != null) {
            oldJobThread.toStop(removeOldReason);
            oldJobThread.interrupt();
            return oldJobThread;
        }
        return null;
    }

    /**
     * 获取对应的任务id的任务线程.
     *
     * @param jobId 任务id
     * @return 任务线程
     */
    public static JobThread loadJobThread(int jobId) {
        return jobThreadRepository.get(jobId);
    }

}
