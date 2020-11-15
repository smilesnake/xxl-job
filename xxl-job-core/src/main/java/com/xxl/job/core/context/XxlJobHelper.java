package com.xxl.job.core.context;

import com.xxl.job.core.log.XxlJobFileAppender;
import com.xxl.job.core.util.DateUtil;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * xxlJob帮助器
 *
 * @author xuxueli 2020-11-05
 */
@Slf4j(topic = "xxl-job logger")
public class XxlJobHelper {

  // ---------------------- base info ----------------------

  /**
   * 获取当前任务id
   *
   * @return 当前任务id
   */
  public static long getJobId() {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return -1;
    }

    return xxlJobContext.getJobId();
  }

  /**
   * 获取当前任务参数
   *
   * @return 当前任务参数
   */
  public static String getJobParam() {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return null;
    }

    return xxlJobContext.getJobParam();
  }

  // ---------------------- for log ----------------------

  /**
   * 获取当前任务参数任务日志文件名称
   *
   * @return 当前任务参数任务日志文件名称
   */
  public static String getJobLogFileName() {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return null;
    }

    return xxlJobContext.getJobLogFileName();
  }

  // ---------------------- for shard ----------------------

  /**
   * 获取当前分片序号
   *
   * @return 当前分片序号
   */
  public static int getShardIndex() {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return -1;
    }

    return xxlJobContext.getShardIndex();
  }

  /**
   * 获取当前分片总数
   *
   * @return 当前分片总数
   */
  public static int getShardTotal() {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return -1;
    }

    return xxlJobContext.getShardTotal();
  }

  // ---------------------- tool for log ----------------------

  /**
   * 通过表达式模式追加日志append log with pattern
   *
   * @param appendLogPattern   追加日志的表达式模式，例如： "aaa {} bbb {} ccc"
   * @param appendLogArguments 追加日志的参数，例如 "111, true"
   * @return true, 成功，否则false
   */
  public static boolean log(String appendLogPattern, Object... appendLogArguments) {

    FormattingTuple ft = MessageFormatter.arrayFormat(appendLogPattern, appendLogArguments);
    // 追加的完整日志
    String appendLog = ft.getMessage();

    StackTraceElement callInfo = new Throwable().getStackTrace()[1];
    return logDetail(callInfo, appendLog);
  }

  /**
   * 追加异常堆栈
   *
   * @param e 异常对象
   */
  public static boolean log(Throwable e) {

    StringWriter stringWriter = new StringWriter();
    e.printStackTrace(new PrintWriter(stringWriter));
    String appendLog = stringWriter.toString();

    StackTraceElement callInfo = new Throwable().getStackTrace()[1];
    return logDetail(callInfo, appendLog);
  }

  /**
   * 追加日志
   *
   * @param callInfo  调用明细
   * @param appendLog 追加日志
   * @return true, 成功，否则false
   */
  private static boolean logDetail(StackTraceElement callInfo, String appendLog) {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return false;
    }

        /*// "yyyy-MM-dd HH:mm:ss [ClassName]-[MethodName]-[LineNumber]-[ThreadName] log";
        StackTraceElement[] stackTraceElements = new Throwable().getStackTrace();
        StackTraceElement callInfo = stackTraceElements[1];*/

    // 追加日志
    StringBuffer stringBuffer = new StringBuffer();
    stringBuffer.append(DateUtil.formatDateTime(new Date())).append(" ")
        .append("[" + callInfo.getClassName() + "#" + callInfo.getMethodName() + "]").append("-")
        .append("[" + callInfo.getLineNumber() + "]").append("-")
        .append("[" + Thread.currentThread().getName() + "]").append(" ")
        .append(appendLog != null ? appendLog : "");
    String formatAppendLog = stringBuffer.toString();

    // appendlog
    String logFileName = xxlJobContext.getJobLogFileName();

    if (StringUtils.isNotBlank(logFileName)) {
      XxlJobFileAppender.appendLog(logFileName, formatAppendLog);
      return true;
    } else {
      log.info(">>>>>>>>>>> {}", formatAppendLog);
      return false;
    }
  }

  // ---------------------- tool for handleResult ----------------------

  /**
   * 处理成功
   *
   * @return true, 处理成功，否则，false
   */
  public static boolean handleSuccess() {
    return handleResult(XxlJobContext.HANDLE_COCE_SUCCESS, null);
  }

  /**
   * 处理成功
   *
   * @param handleMsg 处理消息
   * @return true, 处理成功，否则，false
   */
  public static boolean handleSuccess(String handleMsg) {
    return handleResult(XxlJobContext.HANDLE_COCE_SUCCESS, handleMsg);
  }

  /**
   * 处理失败
   *
   * @return true, 处理失败，否则，false
   */
  public static boolean handleFail() {
    return handleResult(XxlJobContext.HANDLE_COCE_FAIL, null);
  }

  /**
   * 处理失败
   *
   * @param handleMsg 处理消息
   * @return true, 处理失败，否则，false
   */
  public static boolean handleFail(String handleMsg) {
    return handleResult(XxlJobContext.HANDLE_COCE_FAIL, handleMsg);
  }

  /**
   * 处理超时
   *
   * @return true, 超时，否则，false
   */
  public static boolean handleTimeout() {
    return handleResult(XxlJobContext.HANDLE_COCE_TIMEOUT, null);
  }

  /**
   * 处理超时
   *
   * @param handleMsg 处理消息
   * @return true, 超时，否则，false
   */
  public static boolean handleTimeout(String handleMsg) {
    return handleResult(XxlJobContext.HANDLE_COCE_TIMEOUT, handleMsg);
  }

  /**
   * 处理结果
   *
   * @param handleCode 处理码 （200 : 成功 ，500 : 失败， 502 : 超时）
   * @param handleMsg  处理消息
   * @return
   */
  public static boolean handleResult(int handleCode, String handleMsg) {
    XxlJobContext xxlJobContext = XxlJobContext.getXxlJobContext();
    if (xxlJobContext == null) {
      return false;
    }

    // 设置处理码
    xxlJobContext.setHandleCode(handleCode);
    // 设置处理消息
    if (handleMsg != null) {
      xxlJobContext.setHandleMsg(handleMsg);
    }
    return true;
  }


}
