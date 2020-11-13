package com.xxl.job.admin.core.thread;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.core.biz.model.RegistryParam;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 任务注册监听器帮助类实例
 *
 * @author xuxueli 2016-10-02 19:10:24
 */
@Slf4j
public class JobRegistryHelper {

  private static JobRegistryHelper instance = new JobRegistryHelper();

  public static JobRegistryHelper getInstance() {
    return instance;
  }

  /**
   * 测试或删除线程池.
   */
  private ThreadPoolExecutor registryOrRemoveThreadPool = null;
  /**
   * 删除线程.
   */
  private Thread registryMonitorThread;
  /**
   * 尝试停止
   */
  private volatile boolean toStop = false;

  /**
   * start，任务启动，帮助刷新注册上来的服务信息，即appname、address等
   */
  public void start() {

    // 注册或移除线程池
    registryOrRemoveThreadPool = new ThreadPoolExecutor(2, 10, 30L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(2000), r -> new Thread(r,
        "xxl-job, admin JobRegistryMonitorHelper-registryOrRemoveThreadPool-" + r.hashCode()),
        (r, executor) -> {
          r.run();
          log.warn(
              ">>>>>>>>>>> xxl-job, registry or remove too fast, match threadpool rejected handler(run now).");
        });

    // for monitor
    registryMonitorThread = new Thread(() -> {
      while (!toStop) {
        try {
          //自动注入的执行器列表
          List<XxlJobGroup> groupList = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao()
              .findByAddressType(0);
          if (groupList != null && !groupList.isEmpty()) {

            // remove dead address (admin/executor)
            // 移除已经不在线的地址（系统执行器）
            List<Integer> ids = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
                .findDead(RegistryConfig.DEAD_TIMEOUT, new Date());
            if (ids != null && ids.size() > 0) {
              XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao().removeDead(ids);
            }

            // fresh online address (admin/executor)
            // 刷新在线的地址（系统执行器）
            HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
            List<XxlJobRegistry> list = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
                .findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
            if (list != null) {
              for (XxlJobRegistry item : list) {
                if (RegistryConfig.RegistryType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                  String appname = item.getRegistryKey();
                  List<String> registryList = appAddressMap.get(appname);
                  if (registryList == null) {
                    registryList = new ArrayList<String>();
                  }
                  //	不存在注册的服务器url地址直接添加
                  if (!registryList.contains(item.getRegistryValue())) {
                    registryList.add(item.getRegistryValue());
                  }
                  appAddressMap.put(appname, registryList);
                }
              }
            }

            // fresh group address
            // 刷新执行器地址信息
            for (XxlJobGroup group : groupList) {
              List<String> registryList = appAddressMap.get(group.getAppname());
              String addressListStr = null;
              if (registryList != null && !registryList.isEmpty()) {
                Collections.sort(registryList);
                StringBuilder addressListSB = new StringBuilder();
                for (String item : registryList) {
                  addressListSB.append(item).append(",");
                }
                addressListStr = addressListSB.toString();
                addressListStr = addressListStr.substring(0, addressListStr.length() - 1);
              }
              group.setAddressList(addressListStr);
              group.setUpdateTime(new Date());

              XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().update(group);
            }
          }
        } catch (Exception e) {
          if (!toStop) {
            log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
          }
        }
        try {
          TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
        } catch (InterruptedException e) {
          if (!toStop) {
            log.error(">>>>>>>>>>> xxl-job, job registry monitor thread error:{}", e);
          }
        }
      }
      log.info(">>>>>>>>>>> xxl-job, job registry monitor thread stop");
    });
    registryMonitorThread.setDaemon(true);
    registryMonitorThread.setName("xxl-job, admin JobRegistryMonitorHelper-registryMonitorThread");
    registryMonitorThread.start();
  }

  /**
   * 停止
   */
  public void toStop() {
    toStop = true;

    // stop registryOrRemoveThreadPool
    registryOrRemoveThreadPool.shutdownNow();

    // stop monitir (interrupt and wait)
    registryMonitorThread.interrupt();
    try {
      registryMonitorThread.join();
    } catch (InterruptedException e) {
      log.error(e.getMessage(), e);
    }
  }

  // ---------------------- helper ----------------------

  /**
   * 注册
   *
   * @param registryParam 注册参数
   * @return 回调成功，ReturnT.SUCCESS，否则ReturnT.FAIL_CODE
   */
  public ReturnT<String> registry(RegistryParam registryParam) {

    // 校验
    if (!StringUtils.hasText(registryParam.getRegistryGroup())
        || !StringUtils.hasText(registryParam.getRegistryKey())
        || !StringUtils.hasText(registryParam.getRegistryValue())) {
      return new ReturnT<>(ReturnT.FAIL_CODE, "Illegal Argument.");
    }

    // 异步执行, 更新时间，不存在则添加，然后刷新注册信息
    registryOrRemoveThreadPool.execute(() -> {
      int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
          .registryUpdate(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),
              registryParam.getRegistryValue(), new Date());
      if (ret < 1) {
        XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
            .registrySave(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),
                registryParam.getRegistryValue(), new Date());

        // 刷新注册信息
        freshGroupRegistryInfo(registryParam);
      }
    });

    return ReturnT.SUCCESS;
  }

  /**
   * 移除注册
   *
   * @param registryParam 注册参数
   * @return 回调成功，ReturnT.SUCCESS，否则ReturnT.FAIL_CODE
   */
  public ReturnT<String> registryRemove(RegistryParam registryParam) {

    // 校验
    if (!StringUtils.hasText(registryParam.getRegistryGroup())
        || !StringUtils.hasText(registryParam.getRegistryKey())
        || !StringUtils.hasText(registryParam.getRegistryValue())) {
      return new ReturnT<String>(ReturnT.FAIL_CODE, "Illegal Argument.");
    }

    // 删除注册信息
    registryOrRemoveThreadPool.execute(new Runnable() {
      @Override
      public void run() {
        int ret = XxlJobAdminConfig.getAdminConfig().getXxlJobRegistryDao()
            .registryDelete(registryParam.getRegistryGroup(), registryParam.getRegistryKey(),
                registryParam.getRegistryValue());
        if (ret > 0) {
          // 刷新注册信息
          freshGroupRegistryInfo(registryParam);
        }
      }
    });

    return ReturnT.SUCCESS;
  }

  /**
   * 刷新注册信息
   *
   * @param registryParam 注册参数
   */
  private void freshGroupRegistryInfo(RegistryParam registryParam) {
    // Under consideration, prevent affecting core tables
  }


}
