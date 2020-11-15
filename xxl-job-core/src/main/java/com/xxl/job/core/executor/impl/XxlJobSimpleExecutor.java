package com.xxl.job.core.executor.impl;

import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.util.CollectionUtils;


/**
 * 无框架 xxl-job执行器
 *
 * @author xuxueli 2020-11-05
 */
@Slf4j
public class XxlJobSimpleExecutor extends XxlJobExecutor {

  /**
   * xxlJob Bean列表
   */
  @Getter
  @Setter
  private List<Object> xxlJobBeanList = new ArrayList<>();

  @Override
  public void start() {

    // init JobHandler Repository (for method)
    initJobHandlerMethodRepository(xxlJobBeanList);

    // super start
    try {
      super.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroy() {
    super.destroy();
  }

  /**
   * 初始化任务处理器
   *
   * @param xxlJobBeanList xxlJob Bean列表
   */
  private void initJobHandlerMethodRepository(List<Object> xxlJobBeanList) {
    if (!CollectionUtils.isEmpty(xxlJobBeanList)) {
      return;
    }

    // 通过方法初始化任务处理器
    for (Object bean : xxlJobBeanList) {
      // 反射的方法
      Method[] methods = bean.getClass().getDeclaredMethods();
      if (ArrayUtils.isEmpty(methods)) {
        continue;
      }
      for (Method executeMethod : methods) {

        // 获取注解
        XxlJob xxlJob = executeMethod.getAnnotation(XxlJob.class);
        if (xxlJob == null) {
          continue;
        }

        String name = xxlJob.value();
        if (name.trim().length() == 0) {
          throw new RuntimeException(
              String.format("xxl-job method-jobHandler name invalid, for[%s#%s] .", bean.getClass(),
                  executeMethod.getName()));
        }
        if (loadJobHandler(name) != null) {
          throw new RuntimeException("xxl-job jobHandler[" + name + "] naming conflicts.");
        }

        // execute method
                /*if (!(method.getParameterTypes().length == 1 && method.getParameterTypes()[0].isAssignableFrom(String.class))) {
                    throw new RuntimeException("xxl-job method-jobhandler param-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                            "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                }
                if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
                    throw new RuntimeException("xxl-job method-jobhandler return-classtype invalid, for[" + bean.getClass() + "#" + method.getName() + "] , " +
                            "The correct method format like \" public ReturnT<String> execute(String param) \" .");
                }*/

        executeMethod.setAccessible(true);

        // init and destory
        Method initMethod = null;
        Method destroyMethod = null;

        //验证init()方法与destroy方法是否存在，配置了不存在直接抛出异常
        if (StringUtils.isNotBlank(xxlJob.init())) {
          try {
            initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
            initMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#"
                    + executeMethod.getName() + "] .");
          }
        }
        if (StringUtils.isNotBlank(xxlJob.destroy())) {
          try {
            destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
            destroyMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#"
                    + executeMethod.getName() + "] .");
          }
        }
        // registry jobhandler
        registJobHandler(name, new MethodJobHandler(bean, executeMethod, initMethod, destroyMethod));
      }
    }
  }
}
