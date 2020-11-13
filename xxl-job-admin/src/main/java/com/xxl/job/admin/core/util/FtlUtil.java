package com.xxl.job.admin.core.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import lombok.extern.slf4j.Slf4j;

/**
 * ftl工具类.
 *
 * @author xuxueli 2018-01-17 20:37:48
 */
@Slf4j
public class FtlUtil {
    private FtlUtil() {
    }

    /**
     * 对象包装器，BeansWrapper.getDefaultInstance()已过期
     */
    private static BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS).build();

    /**
     * 生成返回TemplateHashModel对象.
     *
     * @param packageName 全路径名 例如  String.class.getName() -> returns "java.lang.String"
     * @return 返回TemplateHashModel, 可以用来创建哈希表模型来访问任意类的静态方法和字段
     */
    public static TemplateHashModel generateStaticModel(String packageName) {
        try {
            // 返回的 TemplateHashModel 可以用来创建哈希表模型来访问任意类的静态方法和字段
            TemplateHashModel staticModels = wrapper.getStaticModels();
            return (TemplateHashModel) staticModels.get(packageName);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

}
