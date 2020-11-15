package com.xxl.job.core.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * xxl-job 上下文
 *
 * @author xuxueli 2020-05-21 [Dear hj]
 */
@Getter
public class XxlJobContext {

  /**
   * 处理成功状态码
   */
  public static final int HANDLE_COCE_SUCCESS = 200;
  /**
   * 处理失败状态码
   */
  public static final int HANDLE_COCE_FAIL = 500;
  /**
   * 处理超时状态码
   */
  public static final int HANDLE_COCE_TIMEOUT = 502;
  // ---------------------- base info ----------------------
  /**
   * 任务id
   */
  private final long jobId;

  /**
   * 任务日志参数
   */
  private final String jobParam;
  // ---------------------- for log ----------------------
  /**
   * 任务日志文件名称
   */
  private final String jobLogFileName;
  // ---------------------- for shard ----------------------
  /**
   * 分片序号
   */
  private final int shardIndex;

  /**
   * 分片总数
   */
  private final int shardTotal;
  // ---------------------- for handle ----------------------
  /**
   * 处理状态码：执行任务的结果状态
   * <p>
   * 200 : success 500 : fail 502 : timeout
   */
  @Setter
  private int handleCode;

  /**
   * handleMsg: 作业执行的简单日志
   */
  @Setter
  private String handleMsg;


  public XxlJobContext(long jobId, String jobParam, String jobLogFileName, int shardIndex,
      int shardTotal) {
    this.jobId = jobId;
    this.jobParam = jobParam;
    this.jobLogFileName = jobLogFileName;
    this.shardIndex = shardIndex;
    this.shardTotal = shardTotal;

    //默认为成功
    this.handleCode = HANDLE_COCE_SUCCESS;
  }
  // ---------------------- tool ----------------------
  /**
   * 上下文持有者,支持任务处理器的子线程,即父线程生成的变量需要传递到子线程中进行使用
   */
  private static InheritableThreadLocal<XxlJobContext> contextHolder = new InheritableThreadLocal<>();

  public static void setXxlJobContext(XxlJobContext xxlJobContext) {
    contextHolder.set(xxlJobContext);
  }

  public static XxlJobContext getXxlJobContext() {
    return contextHolder.get();
  }

}