package com.xxl.job.core.biz.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回对象通用类.
 *
 * @param <T> 返回内容类型
 * @author xuxueli 2015-12-4 16:32:31
 */
@Data
public class ReturnT<T> implements Serializable {
    public static final long serialVersionUID = 42L;
    /**
     * 成功码.
     */
    public static final int SUCCESS_CODE = 200;
    /**
     * 失败码.
     */
    public static final int FAIL_CODE = 500;
    /**
     * 无内容成功返回对象.
     */
    public static final ReturnT<String> SUCCESS = new ReturnT<>(null);
    /**
     * 无内容失败返回对象.
     */
    public static final ReturnT<String> FAIL = new ReturnT<>(FAIL_CODE, null);
    /**
     * 返回码.
     */
    private int code;
    /**
     * 返回消息.
     */
    private String msg;
    /**
     * 返回内容.
     */
    private T content;

    /**
     * 构造返回对象.
     *
     * @param code 返回码
     * @param msg  返回消息
     */
    public ReturnT(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /**
     * 构造返回对象.
     *
     * @param content 返回内容
     */
    public ReturnT(T content) {
        this.code = SUCCESS_CODE;
        this.content = content;
    }
}
