package com.xxl.job.core.util;

import com.xxl.job.core.context.XxlJobHelper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang.ArrayUtils;

/**
 * <ol>
 *     <li>内嵌编译器如"PythonInterpreter"无法引用扩展包，因此推荐使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)；</li>
 *     <li>因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器；</li>
 *     <li>暂时脚本执行日志只能在脚本执行结束后一次性获取，无法保证实时性；因此为确保日志实时性，可改为将脚本打印的日志存储在指定的日志文件上；</li>
 *     <li>python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用prinf日志顺序会错乱</li>
 * </ol>
 *
 * @author xuxueli
 * @date 17/2/25
 */
public class ScriptUtil {

  private ScriptUtil() {
  }

  /**
   * 生成脚本文件
   *
   * @param scriptFileName 脚本文件名称
   * @param content        脚本内容
   * @throws IOException 读写数据异常，抛出
   */
  public static void markScriptFile(String scriptFileName, String content) throws IOException {
    // make file,   filePath/gluesource/666-123456789.py
    try (FileOutputStream fileOutputStream = new FileOutputStream(scriptFileName)) {
      fileOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * 脚本执行，日志文件实时输出
   *
   * @param command    命令
   * @param scriptFile 脚本文件
   * @param logFile    日志文件
   * @param params     参数
   * @return 0=success, 1=error
   */
  public static int execToFile(String command, String scriptFile, String logFile,
      String... params) {

    Thread inputThread = null;
    Thread errThread = null;
    try (FileOutputStream fileOutputStream = new FileOutputStream(logFile, true)) {
      // 组装command
      List<String> cmdArray = new ArrayList<>();
      cmdArray.add(command);
      cmdArray.add(scriptFile);
      if (ArrayUtils.isNotEmpty(params)) {
        cmdArray.addAll(Arrays.asList(params));
      }
      String[] cmdArrayFinal = cmdArray.toArray(new String[0]);

      // 进程执行
      final Process process = Runtime.getRuntime().exec(cmdArrayFinal);

      // 日志线程
      inputThread = new Thread(() -> {
        try {
          copy(process.getInputStream(), fileOutputStream, new byte[1024]);
        } catch (IOException e) {
          XxlJobHelper.log(e);
        }
      });
      // 错误日志线程
      errThread = new Thread(() -> {
        try {
          copy(process.getErrorStream(), fileOutputStream, new byte[1024]);
        } catch (IOException e) {
          XxlJobHelper.log(e);
        }
      });
      inputThread.start();
      errThread.start();

      // 如果需要，使当前线程等待，直到此进程对象表示的进程已终止。如果子进程已终止，则此方法立即返回。
      // 如果子进程尚未终止，调用线程将被阻塞，直到子进程退出,返回退出码：0，成功，1失败
      int exitValue = process.waitFor();

      // log-thread join
      // 等待线程终止
      inputThread.join();
      errThread.join();

      return exitValue;
    } catch (Exception e) {
      XxlJobHelper.log(e);
      return -1;
    } finally {
      if (inputThread != null && inputThread.isAlive()) {
        inputThread.interrupt();
      }
      if (errThread != null && errThread.isAlive()) {
        errThread.interrupt();
      }
    }
  }

  /**
   * 数据流Copy（Input自动关闭，Output不处理）
   *
   * @param inputStream  输入流
   * @param outputStream 输出流
   * @param buffer       需要拷贝的数据缓冲
   * @throws IOException 读写数据异常，抛出
   */
  private static void copy(InputStream inputStream, OutputStream outputStream, byte[] buffer)
      throws IOException {
    try {
      for (; ; ) {
        int res = inputStream.read(buffer);
        if (res == -1) {
          break;
        }
        if (res > 0 && outputStream != null) {
          outputStream.write(buffer, 0, res);
        }
      }
      Objects.requireNonNull(outputStream).flush();
      // 输出流被flush,此时为空
      inputStream.close();
      inputStream = null;
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }
}