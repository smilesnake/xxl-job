package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobUser;
import com.xxl.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.xxl.job.admin.core.thread.JobScheduleHelper;
import com.xxl.job.admin.core.thread.JobTriggerPoolHelper;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.enums.TriggerTypeEnum;
import com.xxl.job.admin.service.LoginService;
import com.xxl.job.admin.service.XxlJobService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.ExecutorBlockStrategyEnum;
import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.glue.GlueTypeEnum;
import com.xxl.job.core.util.DateUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 任务管理模块
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Slf4j
@Controller
@RequestMapping("/jobinfo")
public class JobInfoController {

  @Resource
  private XxlJobGroupDao xxlJobGroupDao;
  @Resource
  private XxlJobService xxlJobService;

  /**
   * 加载任务信息页面渲染.
   *
   * @param request  request请求体
   * @param model    视图
   * @param jobGroup 执行器id
   * @return 指定视图页面
   */
  @RequestMapping
  public String index(HttpServletRequest request, Model model,
      @RequestParam(required = false, defaultValue = "-1") int jobGroup) {

    // 枚举-字典
    // 路由策略-列表
    model.addAttribute("ExecutorRouteStrategyEnum", ExecutorRouteStrategyEnum.values());
    // Glue类型-字典
    model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());
    // 阻塞处理策略-字典
    model.addAttribute("ExecutorBlockStrategyEnum", ExecutorBlockStrategyEnum.values());

    // 执行器列表
    List<XxlJobGroup> allJobGroupList = xxlJobGroupDao.findAll();

    // filter group
    List<XxlJobGroup> jobGroupList = filterJobGroupByRole(request, allJobGroupList);
    if (CollectionUtils.isEmpty(jobGroupList)) {
      throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
    }
    //执行器列表
    model.addAttribute("JobGroupList", jobGroupList);
    //执行器信息
    model.addAttribute("jobGroup", jobGroup);

    return "jobinfo/jobinfo.index";
  }

  /**
   * 通过角色过滤任务执行器.
   *
   * @param request         request请求体
   * @param allJobGroupList 所有的执行器列表
   * @return 过滤后的任务执行器列表
   */
  static List<XxlJobGroup> filterJobGroupByRole(HttpServletRequest request,
      List<XxlJobGroup> allJobGroupList) {
    List<XxlJobGroup> jobGroupList = new ArrayList<>();
    if (!CollectionUtils.isEmpty(allJobGroupList)) {
      XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
      // 管理员加载所有任务
      if (loginUser.getRole() == 1) {
        jobGroupList = allJobGroupList;
      } else {
        // 普通用户显示自己有的执行器权限
        List<String> groupIdList = new ArrayList<>();
        if (StringUtils.isNotBlank(loginUser.getPermission())) {
          groupIdList = Arrays.asList(loginUser.getPermission().trim().split(","));
        }
        for (XxlJobGroup groupItem : allJobGroupList) {
          if (groupIdList.contains(String.valueOf(groupItem.getId()))) {
            jobGroupList.add(groupItem);
          }
        }
      }
    }
    return jobGroupList;
  }

  /**
   * 验证权限
   *
   * @param request  request请求体
   * @param jobGroup 执行器id
   */
  static void validPermission(HttpServletRequest request, int jobGroup) {
    XxlJobUser loginUser = (XxlJobUser) request.getAttribute(LoginService.LOGIN_IDENTITY_KEY);
    if (!loginUser.validPermission(jobGroup)) {
      throw new XxlJobException(
          I18nUtil.getString("system_permission_limit") + "[username=" + loginUser.getUsername()
              + "]");
    }
  }

  /**
   * 任务分页列表.
   *
   * @param start           页码
   * @param length          页面大小
   * @param jobGroup        执行器主键ID
   * @param triggerStatus   调度状态：0-停止，1-运行
   * @param jobDesc         任务描述
   * @param executorHandler 执行器，任务Handler名称
   * @param author          负责人
   * @return 任务分页列表
   */
  @GetMapping("/pageList")
  @ResponseBody
  public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,
      @RequestParam(required = false, defaultValue = "10") int length, int jobGroup,
      int triggerStatus, String jobDesc, String executorHandler, String author) {

    return xxlJobService
        .pageList(start, length, jobGroup, triggerStatus, jobDesc, executorHandler, author);
  }

  /**
   * 新增任务信息
   *
   * @param jobInfo 任务信息实体
   * @return 新增的任务id
   */
  @RequestMapping("/add")
  @ResponseBody
  public ReturnT<String> add(XxlJobInfo jobInfo) {
    return xxlJobService.add(jobInfo);
  }

  /**
   * 更新任务信息
   *
   * @param jobInfo 任务信息实体
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/update")
  @ResponseBody
  public ReturnT<String> update(XxlJobInfo jobInfo) {
    return xxlJobService.update(jobInfo);
  }

  /**
   * 删除任务信息
   *
   * @param id 任务信息主键id
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/remove")
  @ResponseBody
  public ReturnT<String> remove(int id) {
    return xxlJobService.remove(id);
  }

  /**
   * 停止
   *
   * @param id 任务信息主键id
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/stop")
  @ResponseBody
  public ReturnT<String> pause(int id) {
    return xxlJobService.stop(id);
  }

  /**
   * 启动
   *
   * @param id 任务信息主键id
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/start")
  @ResponseBody
  public ReturnT<String> start(int id) {
    return xxlJobService.start(id);
  }

  /**
   * 执行一次（触发）操作
   *
   * @param id            任务id
   * @param executorParam 执行参数
   * @param addressList   机器地址,为空自动获取
   * @return 调度的结果
   */
  @PostMapping("/trigger")
  @ResponseBody
  public ReturnT<String> triggerJob(int id, String executorParam, String addressList) {
    // force cover job param
    if (executorParam == null) {
      executorParam = "";
    }

    JobTriggerPoolHelper.trigger(id, TriggerTypeEnum.MANUAL, -1, null, executorParam, addressList);
    return ReturnT.SUCCESS;
  }

  /**
   * 下一次的执行时间
   *
   * @param scheduleType cron表达式
   * @param scheduleConf cron表达式
   * @return 下次执行时间列表
   */
  @RequestMapping("/nextTriggerTime")
  @ResponseBody
  public ReturnT<List<String>> nextTriggerTime(String scheduleType, String scheduleConf) {

    XxlJobInfo paramXxlJobInfo = new XxlJobInfo();
    paramXxlJobInfo.setScheduleType(scheduleType);
    paramXxlJobInfo.setScheduleConf(scheduleConf);

    List<String> result = new ArrayList<>();
    try {
      //示例次数
      final int samplesFrequency = 5;
      Date lastTime = new Date();
      for (int i = 0; i < samplesFrequency; i++) {
        lastTime = JobScheduleHelper.generateNextValidTime(paramXxlJobInfo, lastTime);
        if (lastTime != null) {
          result.add(DateUtil.formatDateTime(lastTime));
        } else {
          break;
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return new ReturnT<>(ReturnT.FAIL_CODE,
          (I18nUtil.getString("schedule_type") + I18nUtil.getString("system_unvalid")) + e
              .getMessage());
    }
    return new ReturnT<>(result);

  }

}
