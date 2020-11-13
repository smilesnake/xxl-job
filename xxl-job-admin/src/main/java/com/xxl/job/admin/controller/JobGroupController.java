package com.xxl.job.admin.controller;

import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobRegistry;
import com.xxl.job.admin.core.util.I18nUtil;
import com.xxl.job.admin.dao.XxlJobGroupDao;
import com.xxl.job.admin.dao.XxlJobInfoDao;
import com.xxl.job.admin.dao.XxlJobRegistryDao;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.enums.RegistryConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 执行器管理
 *
 * @author xuxueli 2016-10-02 20:52:56
 */
@Controller
@RequestMapping("/jobgroup")
public class JobGroupController {

    @Resource
    public XxlJobInfoDao xxlJobInfoDao;
    @Resource
    public XxlJobGroupDao xxlJobGroupDao;
    @Resource
    private XxlJobRegistryDao xxlJobRegistryDao;

    /**
     * 执行器管理首页
     *
     * @return 视图地址
     */
    @GetMapping
    public String index() {
        return "jobgroup/jobgroup.index";
    }

    /**
     * 执行器列表分页.
     *
     * @param request
     * @param start   页码
     * @param length  页面大小
     * @param appname 执行器名称
     * @param title   执行器标题
     * @return
     */
    @RequestMapping("/pageList")
    @ResponseBody
    public Map<String, Object> pageList(HttpServletRequest request,
                                        @RequestParam(required = false, defaultValue = "0") int start,
                                        @RequestParam(required = false, defaultValue = "10") int length,
                                        String appname, String title) {

        // 分页查询
        List<XxlJobGroup> list = xxlJobGroupDao.pageList(start, length, appname, title);
        int count = xxlJobGroupDao.pageListCount(start, length, appname, title);

        // package result
        Map<String, Object> maps = new HashMap<>();
        // 总记录数
        maps.put("recordsTotal", count);
        // 过滤后的总记录数
        maps.put("recordsFiltered", count);
        // 分页列表
        maps.put("data", list);
        return maps;
    }

    /**
     * 保存执行器信息
     *
     * @param xxlJobGroup 执行器信息
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    @RequestMapping("/save")
    @ResponseBody
    public ReturnT<String> save(XxlJobGroup xxlJobGroup) {
        final int minLen = 4;
        final int maxLen = 64;

        // valid
        if (StringUtils.isBlank(xxlJobGroup.getAppname())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (xxlJobGroup.getAppname().length() < minLen || xxlJobGroup.getAppname().length() > maxLen) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (StringUtils.isBlank(xxlJobGroup.getTitle())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }
        if (xxlJobGroup.getAddressType() != 0) {
            if (StringUtils.isBlank(xxlJobGroup.getAddressList())) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            String[] address = xxlJobGroup.getAddressList().split(",");
            for (String item : address) {
                if (StringUtils.isBlank(item)) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }

		// process
		xxlJobGroup.setUpdateTime(new Date());

		int ret = xxlJobGroupDao.save(xxlJobGroup);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

    /**
     * 更新执行器信息
     *
     * @param xxlJobGroup 执行器信息
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    @RequestMapping("/update")
    @ResponseBody
    public ReturnT<String> update(XxlJobGroup xxlJobGroup) {
        final int minLen = 4;
        final int maxLen = 64;
        // valid
        if (StringUtils.isBlank(xxlJobGroup.getAppname())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + "AppName"));
        }
        if (xxlJobGroup.getAppname().length() < minLen || xxlJobGroup.getAppname().length() > maxLen) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_appname_length"));
        }
        if (StringUtils.isBlank(xxlJobGroup.getTitle())) {
            return new ReturnT<>(500, (I18nUtil.getString("system_please_input") + I18nUtil.getString("jobgroup_field_title")));
        }

        if (xxlJobGroup.getAddressType() == 0) {
            // 0=自动注册
            List<String> registryList = findRegistryByAppName(xxlJobGroup.getAppname());
            StringBuilder addressListStr = new StringBuilder();
            if (!CollectionUtils.isEmpty(registryList)) {
                Collections.sort(registryList);
                for (String item : registryList) {
                    addressListStr.append(item).append(",");
                }
                addressListStr = new StringBuilder(addressListStr.substring(0, addressListStr.length() - 1));
            }
            xxlJobGroup.setAddressList(addressListStr.toString());
        } else {
            // 1=手动录入
            if (StringUtils.isBlank(xxlJobGroup.getAddressList())) {
                return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_addressType_limit"));
            }
            String[] address = xxlJobGroup.getAddressList().split(",");
            for (String item : address) {
                if (StringUtils.isBlank(item)) {
                    return new ReturnT<>(500, I18nUtil.getString("jobgroup_field_registryList_unvalid"));
                }
            }
        }

		// process
		xxlJobGroup.setUpdateTime(new Date());

		int ret = xxlJobGroupDao.update(xxlJobGroup);
		return (ret>0)?ReturnT.SUCCESS:ReturnT.FAIL;
	}

    /**
     * 通过执行器名称找到注册的执行器列表
     *
     * @param appNameParam 执行器名称
     * @return 在线的执行器列表信息
     */
    private List<String> findRegistryByAppName(String appNameParam) {
        HashMap<String, List<String>> appAddressMap = new HashMap<>();
        //在线的执行器信息
        List<XxlJobRegistry> list = xxlJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT, new Date());
        if (!CollectionUtils.isEmpty(list)) {
            for (XxlJobRegistry item : list) {
                if (RegistryConfig.RegistryType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                    String appName = item.getRegistryKey();
                    List<String> registryList = appAddressMap.get(appName);
                    if (registryList == null) {
                        registryList = new ArrayList<>();
                    }

                    if (!registryList.contains(item.getRegistryValue())) {
                        registryList.add(item.getRegistryValue());
                    }
                    appAddressMap.put(appName, registryList);
                }
            }
        }
        return appAddressMap.get(appNameParam);
    }

    /**
     * 删除执行器信息
     *
     * @param id 执行器id
     * @return ReturnT.SUCCESS，成功，否则，失败
     * @see ReturnT#SUCCESS
     * @see ReturnT#FAIL
     */
    @RequestMapping("/remove")
    @ResponseBody
    public ReturnT<String> remove(int id) {

        // valid
        int count = xxlJobInfoDao.pageListCount(id, -1, null, null, null);
        if (count > NumberUtils.INTEGER_ZERO) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_0"));
        }

        List<XxlJobGroup> allList = xxlJobGroupDao.findAll();
        if (allList.size() == NumberUtils.INTEGER_ONE) {
            return new ReturnT<>(500, I18nUtil.getString("jobgroup_del_limit_1"));
        }

        int ret = xxlJobGroupDao.remove(id);
        return (ret > 0) ? ReturnT.SUCCESS : ReturnT.FAIL;
    }

    /**
     * 通过执行器id加载执行器信息
     *
     * @param id 执行器id
     * @return 成功，返回执行器信息，否则，ReturnT.FAIL_CODE
     * @see ReturnT#FAIL_CODE
     */
    @RequestMapping("/loadById")
    @ResponseBody
    public ReturnT<XxlJobGroup> loadById(int id) {
        XxlJobGroup jobGroup = xxlJobGroupDao.load(id);
        return jobGroup != null ? new ReturnT<>(jobGroup) : new ReturnT<>(ReturnT.FAIL_CODE, null);
    }

}
