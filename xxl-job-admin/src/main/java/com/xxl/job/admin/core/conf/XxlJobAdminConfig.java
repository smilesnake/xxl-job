package com.xxl.job.admin.core.conf;

import com.xxl.job.admin.core.alarm.JobAlarmer;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.dao.XxlJobLogReportDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import java.util.Arrays;
import javax.annotation.Resource;
import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * xxl-job 系统配置
 *
 * @author xuxueli 2017-04-28
 */

@Component
public class XxlJobAdminConfig implements InitializingBean, DisposableBean {

  @Getter
  private static XxlJobAdminConfig adminConfig;

  // ---------------------- XxlJobScheduler ----------------------
  /**
   * 任务定时器.
   */
  private XxlJobScheduler xxlJobScheduler;

  @Override
  public void afterPropertiesSet() {
    //对执行器进行初始化
    XxlJobAdminConfig.adminConfig = this;
    xxlJobScheduler = new XxlJobScheduler();
    xxlJobScheduler.init();
  }

  @Override
  public void destroy() {
    xxlJobScheduler.destroy();
  }

  // ---------------------- XxlJobScheduler ----------------------

  // conf
  /**
   * 调度中心国际化配置 [必填]： 默认为 "zh_CN"/中文简体, 可选范围为 "zh_CN"/中文简体, "zh_TC"/中文繁体 and "en"/英文
   */
  @Value("${xxl.job.i18n}")
  private String i18n;
  /**
   * 调度中心通讯TOKEN [选填]：非空时启用
   */
  @Getter
  @Value("${xxl.job.accessToken}")
  private String accessToken;
  /**
   * 发送方
   */
  @Getter
  @Value("${spring.mail.from}")
  private String emailFrom;
  /**
   * 快线程池最大核心线程数（至少200）
   */
  @Value("${xxl.job.triggerpool.fast.max}")
  private int triggerPoolFastMax;
  /**
   * 慢线程池最大核心线程数（至少100）
   */
  @Value("${xxl.job.triggerpool.slow.max}")
  private int triggerPoolSlowMax;
  /**
   * 日志保留天数（至少7天）
   */
  @Value("${xxl.job.logretentiondays}")
  private int logRetentionDays;

  @Getter
  @Resource
  private XxlJobLogDao xxlJobLogDao;
  @Getter
  @Resource
  private XxlJobInfoDao xxlJobInfoDao;
  @Getter
  @Resource
  private XxlJobRegistryDao xxlJobRegistryDao;
  @Getter
  @Resource
  private XxlJobGroupDao xxlJobGroupDao;
  @Getter
  @Resource
  private XxlJobLogReportDao xxlJobLogReportDao;
  @Getter
  @Resource
  private JavaMailSender mailSender;
  @Getter
  @Resource
  private DataSource dataSource;
  @Getter
  @Resource
  private JobAlarmer jobAlarmer;

  /**
   * 调度中心国际化配置
   *
   * @return 中心国际化配置字符串
   */
  public String getI18n() {
    return !Arrays.asList("zh_CN", "zh_TC", "en").contains(i18n) ? "zh_CN" : i18n;
  }

  /**
   * 快线程池最大核心线程数（至少200）
   *
   * @return 最大核心线程数
   */
  public int getTriggerPoolFastMax() {
    return triggerPoolFastMax < 200 ? 200 : triggerPoolFastMax;
  }

  /**
   * 慢线程池最大核心线程数（至少100）
   *
   * @return 最大核心线程数
   */
  public int getTriggerPoolSlowMax() {
    return triggerPoolSlowMax < 100 ? 100 : triggerPoolSlowMax;
  }

  /**
   * 日志保留天数（至少7天）
   *
   * @return 保留天数
   */
  public int getLogRetentionDays() {
    // 限制大于或等于7，否则不开放
    return logRetentionDays < 7 ? -1 : logRetentionDays;
  }
}
