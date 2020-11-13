package com.xxl.job.core.thread;

import com.xxl.job.core.biz.AdminBiz;
import com.xxl.job.core.biz.model.HandleCallbackParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobContext;
import com.xxl.job.core.enums.RegistryConfig;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.FileUtil;
import com.xxl.job.core.util.JdkSerializeTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 回调调度线程.
 *
 * @author xuxueli
 * @date 16/7/22
 */
@Slf4j
public class TriggerCallbackThread {

    private TriggerCallbackThread() {
    }

    private static TriggerCallbackThread instance = new TriggerCallbackThread();

    public static TriggerCallbackThread getInstance() {
        return instance;
    }

    /**
     * 任务结果回调队列.
     */
    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<>();

    /**
     * 添加回调处理器参数.
     *
     * @param callback 回调处理器参数
     */
    static void pushCallBack(HandleCallbackParam callback) {
        getInstance().callBackQueue.add(callback);
        log.debug(">>>>>>>>>>> xxl-job, push callback request, logId:{}", callback.getLogId());
    }

    /**
     * 回调调度线程.
     */
    private Thread triggerCallbackThread;
    /**
     * 重试调度线程.
     */
    private Thread triggerRetryCallbackThread;
    /**
     * 尝试停止.
     */
    private volatile boolean toStop = false;

    public void start() {

        // 调度中心部署根地址列表没有配置
        if (XxlJobExecutor.getAdminBizList() == null) {
            log.warn(">>>>>>>>>>> xxl-job, executor callback config fail, adminAddresses is null.");
            return;
        }

        // callback
        triggerCallbackThread = new Thread(() -> {

            // normal callback
            while (!toStop) {
                try {
                    HandleCallbackParam callback = getInstance().callBackQueue.take();
                    if (callback != null) {

                        // callback list param
                        List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                        // 移除此队列中所有可用的元素，并将它们添加到给定 callbackParamList中
                        getInstance().callBackQueue.drainTo(callbackParamList);
                        callbackParamList.add(callback);

                        // 回调，失败将会重试
                        if (!CollectionUtils.isEmpty(callbackParamList)) {
                            doCallback(callbackParamList);
                        }
                    }
                } catch (Exception e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }
                }
            }

            // last callback
            try {
                //执行器停止前先上报所有执行中的任务结果
                List<HandleCallbackParam> callbackParamList = new ArrayList<>();
                // 移除此队列中所有可用的元素，并将它们添加到给定 callbackParamList 中
                int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                if (!CollectionUtils.isEmpty(callbackParamList)) {
                    doCallback(callbackParamList);
                }
            } catch (Exception e) {
                if (!toStop) {
                    log.error(e.getMessage(), e);
                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor callback thread destory.");

        });
        triggerCallbackThread.setDaemon(true);
        triggerCallbackThread.setName("xxl-job, executor TriggerCallbackThread");
        triggerCallbackThread.start();


        // 重试
        triggerRetryCallbackThread = new Thread(() -> {
            while (!toStop) {
                try {
                    retryFailCallbackFile();
                } catch (Exception e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                    }

                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    if (!toStop) {
                        log.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
                    }
                }
            }
            log.info(">>>>>>>>>>> xxl-job, executor retry callback thread destory.");
        });
        triggerRetryCallbackThread.setDaemon(true);
        triggerRetryCallbackThread.start();

    }

    /**
     * 停止调度回调线程
     */
    public void toStop() {
        toStop = true;
        // support empty admin address
        // 停止回调线程，中断，等待任务结束后停止
        if (triggerCallbackThread != null) {
            triggerCallbackThread.interrupt();
            try {
                triggerCallbackThread.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

        // 停止重试线程，中断，等待任务结束后停止
        if (triggerRetryCallbackThread != null) {
            triggerRetryCallbackThread.interrupt();
            try {
                triggerRetryCallbackThread.join();
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }

    }

    /**
     * 尝试回调，如果失败将会重试
     *
     * @param callbackParamList 回调参数列表
     */
    private void doCallback(List<HandleCallbackParam> callbackParamList) {
        boolean callbackRetry = false;
        // callback, will retry if error
        //回调，如果失败将会重试
        for (AdminBiz adminBiz : XxlJobExecutor.getAdminBizList()) {
            try {
                //遍历调度端，xxl-rpc发送任务执行结果
                ReturnT<String> callbackResult = adminBiz.callback(callbackParamList);
                if (callbackResult != null && ReturnT.SUCCESS_CODE == callbackResult.getCode()) {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback finish.");
                    callbackRetry = true;
                    break;
                } else {
                    callbackLog(callbackParamList, "<br>----------- xxl-job job callback fail, callbackResult:" + callbackResult);
                }
            } catch (Exception e) {
                callbackLog(callbackParamList, "<br>----------- xxl-job job callback error, errorMsg:" + e.getMessage());
            }
        }
        // 上报失败，则记录到本地上报失败记录文件中，等待重试
        if (!callbackRetry) {
            appendFailCallbackFile(callbackParamList);
        }
    }

    /**
     * 记录回调日志
     */
    private void callbackLog(List<HandleCallbackParam> callbackParamList, String logContent) {
        for (HandleCallbackParam callbackParam : callbackParamList) {
            String logFileName = XxlJobFileAppender.makeLogFileName(Instant.ofEpochMilli(callbackParam
                    .getLogDateTim()).atZone(ZoneOffset.ofHours(8)).toLocalDate(), callbackParam.getLogId());

            XxlJobContext.setXxlJobContext(new XxlJobContext(
                    -1,
                    logFileName,
                    -1,
                    -1));
            XxlJobLogger.log(logContent);
        }
    }


    // ---------------------- fail-callback file ----------------------
    /**
     * 回调失败的文件路径.
     */
    private static String failCallbackFilePath = XxlJobFileAppender.getLogBasePath().concat(File.separator).concat("callbacklog").concat(File.separator);
    /**
     * 回调失败的文件名称.
     */
    private static String failCallbackFileName = failCallbackFilePath.concat("xxl-job-callback-{x}").concat(".log");

    /**
     * 上报失败，则记录到本地上报失败记录文件中.
     *
     * @param callbackParamList 回调参数列表
     */
    private void appendFailCallbackFile(List<HandleCallbackParam> callbackParamList) {
        final Integer maxFileSize = 100;
        // valid
        if (CollectionUtils.isEmpty(callbackParamList)) {
            return;
        }

        // 追加文件
        byte[] callbackParamListBytes = JdkSerializeTool.serialize(callbackParamList);

        File callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis())));
        if (callbackLogFile.exists()) {
            //可能当前时刻文件不止一个
            for (int i = 0; i < maxFileSize; i++) {
                callbackLogFile = new File(failCallbackFileName.replace("{x}", String.valueOf(System.currentTimeMillis()).concat("-").concat(String.valueOf(i))));
                if (!callbackLogFile.exists()) {
                    break;
                }
            }
        }
        FileUtil.writeFileContent(callbackLogFile, callbackParamListBytes);
    }

    /**
     * 重试失败回调文件.
     */
    private void retryFailCallbackFile() {

        // 验证
        File callbackLogPath = new File(failCallbackFilePath);
        if (!callbackLogPath.exists()) {
            return;
        }
        if (callbackLogPath.isFile()) {
            try {
                Files.delete(callbackLogPath.toPath());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        if (!(callbackLogPath.isDirectory() && ArrayUtils.isNotEmpty(callbackLogPath.list()))) {
            return;
        }

        // 加载并清理文件，然后重试
        for (File callbackLogFile : callbackLogPath.listFiles()) {
            byte[] callbackParamListBytes = FileUtil.readFileContent(callbackLogFile);
            List<HandleCallbackParam> callbackParamList = (List<HandleCallbackParam>) JdkSerializeTool.deserialize(callbackParamListBytes);

            try {
                Files.delete(callbackLogPath.toPath());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            doCallback(callbackParamList);
        }

    }

}
