package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLogGlue;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobLogGlueDao;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.exception.XxlJobException;
import com.xxl.job.core.glue.GlueTypeEnum;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * 任务脚本编码管理（WebIDE）
 * <p>代码编辑器</p>
 *
 * @author xuxueli 2015-12-19 16:13:16
 */
@Controller
@RequestMapping("/jobcode")
public class JobCodeController {

    @Resource
    private XxlJobInfoDao xxlJobInfoDao;
    @Resource
    private XxlJobLogGlueDao xxlJobLogGlueDao;

    /**
     * 代码编辑器页面
     *
     * @param request request请求
     * @param model   视图
     * @param jobId   任务id
     * @return 视图地址
     */
    @GetMapping
    public String index(HttpServletRequest request, Model model, Integer jobId) {
        XxlJobInfo jobInfo = xxlJobInfoDao.loadById(jobId);
        List<XxlJobLogGlue> jobLogGlues = xxlJobLogGlueDao.findByJobId(jobId);

        if (jobInfo == null) {
            throw new XxlJobException(I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }
        if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
            throw new XxlJobException(I18nUtil.getString("jobinfo_glue_gluetype_unvalid"));
        }

        // 验证权限
        JobInfoController.validPermission(request, jobInfo.getJobGroup());

        // Glue类型-字典
        model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());
        //任务信息
        model.addAttribute("jobInfo", jobInfo);
        //任务脚本源码记录
        model.addAttribute("jobLogGlues", jobLogGlues);
        return "jobcode/jobcode.index";
    }

    /**
     * 保存任务脚本源码
     *
     * @param id         任务id
     * @param glueSource 脚本源码
     * @param glueRemark 脚本描述（4~100个字符）
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    @PostMapping("/save")
    @ResponseBody
    public ReturnT<String> save(int id, String glueSource, String glueRemark) {
        final int minLen = 4;
        final int maxLen = 100;
        // valid
        if (glueRemark == null) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobinfo_glue_remark")));
        }
        if (glueRemark.length() < minLen || glueRemark.length() > maxLen) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_remark_limit"));
        }
        XxlJobInfo existsJobInfo = xxlJobInfoDao.loadById(id);
        if (existsJobInfo == null) {
            return new ReturnT<>(500, I18nUtil.getString("jobinfo_glue_jobid_unvalid"));
        }

        // 更新最新的脚本源码至任务信息表中
        existsJobInfo.setGlueSource(glueSource);
        existsJobInfo.setGlueRemark(glueRemark);
        existsJobInfo.setGlueUpdatetime(new Date());

        existsJobInfo.setUpdateTime(new Date());

        xxlJobInfoDao.update(existsJobInfo);

        // log old code
        //记录旧版本的任务脚本源码信息
        XxlJobLogGlue xxlJobLogGlue = new XxlJobLogGlue();
        xxlJobLogGlue.setJobId(existsJobInfo.getId());
        xxlJobLogGlue.setGlueType(existsJobInfo.getGlueType());
        xxlJobLogGlue.setGlueSource(glueSource);
        xxlJobLogGlue.setGlueRemark(glueRemark);

        xxlJobLogGlue.setAddTime(new Date());
        xxlJobLogGlue.setUpdateTime(new Date());
        xxlJobLogGlueDao.save(xxlJobLogGlue);

        // remove code backup more than 30
        //永远只保留30条最新记录，移除超过后的旧记录
        xxlJobLogGlueDao.removeOld(xxlJobLogGlue.getId(), 30);

        return ReturnT.SUCCESS;
    }

}
