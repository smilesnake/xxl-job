package com.xxl.job.admin.core.model;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 用户实体
 *
 * @author xuxueli 2019-05-04 16:43:12
 */
@Data
public class XxlJobUser {
    /**
     * 用户id
     */
    private int id;
    /**
     * 账号
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 角色：0-普通用户、1-管理员
     */
    private int role;
    /**
     * 权限：执行器ID列表，多个逗号分割
     */
    private String permission;

    // plugin

    /**
     * 验证权限
     *
     * @param jobGroup 执行器id
     * @return true, 权限通过，否则，权限不足
     */
    public boolean validPermission(int jobGroup) {
        if (this.role == 1) {
            return true;
        } else {
            if (StringUtils.hasText(this.permission)) {
                for (String permissionItem : this.permission.split(",")) {
                    if (String.valueOf(jobGroup).equals(permissionItem)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

}
