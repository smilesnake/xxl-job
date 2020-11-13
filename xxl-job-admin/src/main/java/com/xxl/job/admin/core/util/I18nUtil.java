package com.xxl.job.admin.core.util;

import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Properties国际化I18N工具类.
 *
 * @author xuxueli 2018-01-17 20:39:06
 */
@Slf4j
public class I18nUtil {
    private I18nUtil() {
    }

    /**
     * Properties对象
     */
    private static Properties prop = null;

    /**
     * 加载I18N的Properties.
     *
     * @return Properties对象
     */
    private static Properties loadI18nProp() {
        if (prop != null) {
            return prop;
        }
        try {
            // 构建i18n Properties文件
            String i18n = XxlJobAdminConfig.getAdminConfig().getI18n();
            String i18nFile = MessageFormat.format("i18n/message_{0}.properties", i18n);

            // 加载Properties
            Resource resource = new ClassPathResource(i18nFile);
            EncodedResource encodedResource = new EncodedResource(resource, "UTF-8");
            prop = PropertiesLoaderUtils.loadProperties(encodedResource);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return prop;
    }

    /**
     * 通过i18n的key得到value.
     *
     * @param key i18n的key
     * @return i18n的value
     */
    public static String getString(String key) {
        return loadI18nProp().getProperty(key);
    }

    /**
     * 通过多个i18n的key得到多个value,为空返回全部数据.
     *
     * @param keys 18n的key数组
     * @return 返回key, value的json字符串
     */
    public static String getMultipleString(String... keys) {
        Map<String, String> map = new HashMap<>();

        Properties prop = loadI18nProp();
        if (ArrayUtils.isNotEmpty(keys)) {
            for (String key : keys) {
                map.put(key, prop.getProperty(key));
            }
        } else {
            for (String key : prop.stringPropertyNames()) {
                map.put(key, prop.getProperty(key));
            }
        }

        return JacksonUtil.writeValueAsString(map);
    }
}
