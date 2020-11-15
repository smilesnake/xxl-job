package com.xxl.job.core.executor.impl;

import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.executor.XxlJobExecutor;
import com.xxl.job.core.glue.GlueFactory;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.handler.impl.MethodJobHandler;
import java.lang.reflect.Method;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;


/**
 * spring的xxl-job 执行器,基于XxlJobExecutor实现的
 *
 * @author xuxueli 2018-11-01 09:24:52
 */
@Slf4j
public class XxlJobSpringExecutor extends XxlJobExecutor implements ApplicationContextAware,
    SmartInitializingSingleton, DisposableBean {

  // ---------------------- applicationContext ----------------------
  @Getter
  private static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    XxlJobSpringExecutor.applicationContext = applicationContext;
  }

  // start
  @Override
  public void afterSingletonsInstantiated() {

    // init JobHandler Repository
    /*initJobHandlerRepository(applicationContext);*/

    // 初始化任务处理器方法 (for method)
    initJobHandlerMethodRepository(applicationContext);

    //  刷新GlueFactor
    GlueFactory.refreshInstance(1);

    // 启动任务执行器
    try {
      super.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroy() {
    // 销毁处理
    super.destroy();
  }

  /**
   * 初始化任务处理器方法
   *
   * @param applicationContext applicationContextcc对象
   */
  private void initJobHandlerMethodRepository(ApplicationContext applicationContext) {
    // 没有上下文对象直接返回
    if (applicationContext == null) {
      return;
    }
    // init job handler from method
    // 得到所有注入的对象名称
    String[] beanDefinitionNames = applicationContext
        .getBeanNamesForType(Object.class, false, true);
    for (String beanDefinitionName : beanDefinitionNames) {
      //通过名称获取指定的bean
      Object bean = applicationContext.getBean(beanDefinitionName);
      // 参考org.springframework.context是 啊。事件侦听器方法处理器进程bean
      Map<Method, XxlJob> annotatedMethods = null;
      try {
        //  AnnotatedElementUtils.findMergedAnnotation(element,annotationType),找到方法上的@XxlJob类型注解，没找到返回null
        //  使用MethodIntrospector.selectMethods方法过滤具体的handler method，找到方法上有@XxlJob注解的方法
        annotatedMethods = MethodIntrospector.selectMethods(bean.getClass(),
            (MethodIntrospector.MetadataLookup<XxlJob>) method -> AnnotatedElementUtils
                .findMergedAnnotation(method, XxlJob.class));
      } catch (Throwable ex) {
        log.error("xxl-job method-jobhandler resolve error for bean[" + beanDefinitionName + "].",
            ex);
      }
      if (annotatedMethods == null || annotatedMethods.isEmpty()) {
        continue;
      }

      for (Map.Entry<Method, XxlJob> methodXxlJobEntry : annotatedMethods.entrySet()) {
        //方法
        Method method = methodXxlJobEntry.getKey();
        //指定注解
        XxlJob xxlJob = methodXxlJobEntry.getValue();
        //注解为空直接跳过
        if (xxlJob == null) {
          continue;
        }

        // 任务处理器的名称
        String name = xxlJob.value();
        if (StringUtils.isBlank(name)) {
          throw new RuntimeException(
              "xxl-job method-jobhandler name invalid, for[" + bean.getClass() + "#" + method
                  .getName() + "] .");
        }
        // 名称有冲突
        if (loadJobHandler(name) != null) {
          throw new RuntimeException("xxl-job jobhandler[" + name + "] naming conflicts.");
        }

        // execute method
        //验证执行方法参数格式
/*        if (!(method.getParameterTypes().length == 1 && method.getParameterTypes()[0]
            .isAssignableFrom(String.class))) {
          throw new XxlJobException(
              "xxl-job method-jobhandler param-classtype invalid, for[" + bean.getClass() + "#"
                  + method.getName() + "] , " +
                  "The correct method format like \" public ReturnT<String> execute(String param) \" .");
        }
        if (!method.getReturnType().isAssignableFrom(ReturnT.class)) {
          throw new XxlJobException(
              "xxl-job method-jobhandler return-classtype invalid, for[" + bean.getClass() + "#"
                  + method.getName() + "] , " +
                  "The correct method format like \" public ReturnT<String> execute(String param) \" .");
        }*/
        method.setAccessible(true);

        // init and destory
        Method initMethod = null;
        Method destroyMethod = null;

        //验证init()方法与destroy方法是否存在，配置了不存在直接抛出异常
        if (StringUtils.isNotBlank(xxlJob.init())) {
          try {
            initMethod = bean.getClass().getDeclaredMethod(xxlJob.init());
            initMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            throw new XxlJobException(
                "xxl-job method-jobhandler initMethod invalid, for[" + bean.getClass() + "#"
                    + method.getName() + "] .");
          }
        }
        if (StringUtils.isNotBlank(xxlJob.destroy())) {
          try {
            destroyMethod = bean.getClass().getDeclaredMethod(xxlJob.destroy());
            destroyMethod.setAccessible(true);
          } catch (NoSuchMethodException e) {
            throw new XxlJobException(
                "xxl-job method-jobhandler destroyMethod invalid, for[" + bean.getClass() + "#"
                    + method.getName() + "] .");
          }
        }

        // 注册任务处理器.
        registryJobHandler(name, new MethodJobHandler(bean, method, initMethod, destroyMethod));
      }
    }
  }
}