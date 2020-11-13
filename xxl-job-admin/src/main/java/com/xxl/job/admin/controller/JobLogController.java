package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.admin.core.scheduler.XxlJobScheduler;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogDao;
import com.xxl.job.admin.enums.LogClearType;
import com.xxl.job.core.biz.ExecutorBiz;
import com.xxl.job.core.biz.model.LogResult;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.KillParam;
import com.xxl.job.core.biz.model.remote.LogParam;
import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.util.DateUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 任务调度日志管理
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/joblog")
public class JobLogController {

  private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

  @Resource
  private XxlJobGroupDao xxlJobGroupDao;
  @Resource
  public XxlJobInfoDao xxlJobInfoDao;
  @Resource
  public XxlJobLogDao xxlJobLogDao;

  /**
   * 根据任务id查询任务信息，将数据渲染至指定页面
   *
   * @param request request请求
   * @param model   视图
   * @param jobId   任务id
   * @return 渲染的视图页面
   */
  @RequestMapping
  public String index(HttpServletRequest request, Model model,
      @RequestParam(required = false, defaultValue = "0") Integer jobId) {

    // 执行器列表
    List<XxlJobGroup> allJobGroupList = xxlJobGroupDao.findAll();

    // 过滤执行器
    List<XxlJobGroup> jobGroupList = JobInfoController
        .filterJobGroupByRole(request, allJobGroupList);
    if (CollectionUtils.isEmpty(jobGroupList)) {
      throw new XxlJobException(I18nUtil.getString("jobgroup_empty"));
    }

    model.addAttribute("JobGroupList", jobGroupList);

    // 任务信息
    if (jobId > 0) {
      XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
      if (jobInfo == null) {
        throw new XxlJobException(
            I18nUtil.getString("jobinfo_field_id") + I18nUtil.getString("system_unvalid"));
      }

      model.addAttribute("jobInfo", jobInfo);

      // valid permission
      JobInfoController.validPermission(request, jobInfo.getJobGroup());
    }

    return "joblog/joblog.index";
  }

  /**
   * 通过执行器id找到任务日志信息列表
   *
   * @param jobGroup 执行器id
   * @return 任务日志信息列表
   */
  @RequestMapping("/getJobsByGroup")
  @ResponseBody
  public ReturnT<List<XxlJobInfo>> getJobsByGroup(int jobGroup) {
    List<XxlJobInfo> list = xxlJobInfoDao.getJobsByGroup(jobGroup);
    return new ReturnT<>(list);
  }

  /**
   * 调度日志分页
   *
   * @param request    request请求体
   * @param start      页码
   * @param length     页面大小
   * @param jobGroup   执行器id
   * @param jobId      任务id
   * @param logStatus  日志状态
   * @param filterTime 调度时间（2020-08-30 00:00:00 - 2020-08-30 23:59:59）
   * @return 封装好的数据
   */
  @PostMapping("/pageList")
  @ResponseBody
  public Map<String, Object> pageList(HttpServletRequest request,
      @RequestParam(required = false, defaultValue = "0") int start,
      @RequestParam(required = false, defaultValue = "10") int length, int jobGroup, int jobId,
      int logStatus, String filterTime) {
    final int size = 2;
    // valid permission
    // 仅管理员支持查询全部；普通用户仅支持查询有权限的jobGroup
    JobInfoController.validPermission(request, jobGroup);

    // 解析参数
    Date triggerTimeStart = null;
    Date triggerTimeEnd = null;
    if (StringUtils.isNotBlank(filterTime)) {
      String[] temp = filterTime.split(" - ");
      if (temp.length == size) {
        triggerTimeStart = DateUtil.parseDateTime(temp[0]);
        triggerTimeEnd = DateUtil.parseDateTime(temp[1]);
      }
    }

    // 分页查询
    List<XxlJobLog> list = xxlJobLogDao
        .pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
    int listCount = xxlJobLogDao
        .pageListCount(jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);

    // 包装结果
    Map<String, Object> maps = new HashMap<>();
    // 总记录数
    maps.put("recordsTotal", listCount);
    // 过滤后的总记录数
    maps.put("recordsFiltered", listCount);
    // 分页列表
    maps.put("data", list);
    return maps;
  }

  /**
   * 查看日志详情
   *
   * @param id    日志id
   * @param model 视图
   * @return 视图页面
   */
  @RequestMapping("/logDetailPage")
  public String logDetailPage(int id, Model model) {

    // base check
    XxlJobLog jobLog = xxlJobLogDao.load(id);
    if (jobLog == null) {
      throw new XxlJobException(I18nUtil.getString("joblog_logid_unvalid"));
    }

    model.addAttribute("triggerCode", jobLog.getTriggerCode());
    model.addAttribute("handleCode", jobLog.getHandleCode());
    model.addAttribute("executorAddress", jobLog.getExecutorAddress());
    model.addAttribute("triggerTime", jobLog.getTriggerTime().getTime());
    model.addAttribute("logId", jobLog.getId());
    return "joblog/joblog.detail";
  }

  /**
   * 刷新日志明细（执行日志 Console）
   *
   * @param executorAddress 执行地址
   * @param triggerTime     调度时间
   * @param logId           日志id
   * @param fromLineNum     起始行号
   * @return 任务执行日志
   */
  @RequestMapping("/logDetailCat")
  @ResponseBody
  public ReturnT<LogResult> logDetailCat(String executorAddress, long triggerTime, long logId,
      int fromLineNum) {
    try {
      ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(executorAddress);
      ReturnT<LogResult> logResult = Objects.requireNonNull(executorBiz)
          .log(new LogParam(triggerTime, logId, fromLineNum));

      // 是否结束
      if (logResult.getContent() != null && logResult.getContent().getFromLineNum() > logResult
          .getContent().getToLineNum()) {
        XxlJobLog jobLog = xxlJobLogDao.load(logId);
        if (jobLog.getHandleCode() > 0) {
          logResult.getContent().setEnd(true);
        }
      }
      return logResult;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      return new ReturnT<>(ReturnT.FAIL_CODE, e.getMessage());
    }
  }

  /**
   * 终止任务 （调度日志 - 终止任务）
   *
   * @param id 任务id
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */
  @RequestMapping("/logKill")
  @ResponseBody
  public ReturnT<String> logKill(int id) {
    // base check
    XxlJobLog log = xxlJobLogDao.load(id);
    XxlJobInfo jobInfo = xxlJobInfoDao.loadById(log.getJobId());
    if (jobInfo == null) {
      return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
    }
    if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
      return new ReturnT<>(500, I18nUtil.getString("joblog_kill_log_limit"));
    }

    //kill请求
    ReturnT<String> runResult;
    try {
      // 获取执行器并杀死这个任务
      ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(log.getExecutorAddress());
      runResult = Objects.requireNonNull(executorBiz).kill(new KillParam(jobInfo.getId()));
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      runResult = new ReturnT<>(500, e.getMessage());
    }

    //如果kill成功了，更新任务的状态
    if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
      log.setHandleCode(ReturnT.FAIL_CODE);
      log.setHandleMsg(
          I18nUtil.getString("joblog_kill_log_byman") + ":" + (runResult.getMsg() != null
              ? runResult.getMsg() : ""));
      log.setHandleTime(LocalDate.now());
      xxlJobLogDao.updateHandleInfo(log);
      return new ReturnT<>(runResult.getMsg());
    } else {
      return new ReturnT<>(500, runResult.getMsg());
    }
  }

  /**
   * 清理日志
   *
   * @param jobGroup 执行器id
   * @param jobId    任务id
   * @param type     类型
   * @return ReturnT.SUCCESS，成功，否则，失败
   * @see ReturnT#SUCCESS
   * @see ReturnT#FAIL
   */

  @RequestMapping("/clearLog")
  @ResponseBody
  public ReturnT<String> clearLog(int jobGroup, int jobId, int type) {

    LogClearType clearType = LogClearType.match(type);
    if (clearType == null) {
      return new ReturnT<>(ReturnT.FAIL_CODE, I18nUtil.getString("joblog_clean_type_unvalid"));
    }

    LocalDateTime clearBeforeTime = null;
    int clearBeforeNum = 0;
    if (clearType.isDate()) {
      clearBeforeTime = LocalDateTime.now().plusMonths(clearType.getNumber());
    } else {
      clearBeforeNum = clearType.getNumber();
    }

    List<Long> logIds;
    do {
      logIds = xxlJobLogDao.findClearLogIds(jobGroup, jobId, clearBeforeTime, clearBeforeNum, 1000);
      if (!CollectionUtils.isEmpty(logIds)) {
        xxlJobLogDao.clearLog(logIds);
      }
    } while (!CollectionUtils.isEmpty(logIds));

    return ReturnT.SUCCESS;
  }

}
