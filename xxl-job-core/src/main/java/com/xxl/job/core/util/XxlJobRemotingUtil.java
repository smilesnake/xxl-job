package com.xxl.job.core.util;

import com.xxl.job.core.biz.model.ReturnT;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;

/**
 * XxlJob 远程工具类.
 *
 * @author xuxueli 2018-11-25 00:55:31
 */
@Slf4j
public class XxlJobRemotingUtil {
    private XxlJobRemotingUtil() {
    }

    public static final String XXL_JOB_ACCESS_TOKEN = "XXL-JOB-ACCESS-TOKEN";

    //------------------------------------------ trust-https start ---------------------------------------------------

    /**
     * 信任https调度
     *
     * @param connection https的url连接
     */
    private static void trustAllHosts(HttpsURLConnection connection) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            SSLSocketFactory newFactory = sc.getSocketFactory();

            connection.setSSLSocketFactory(newFactory);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        connection.setHostnameVerifier((hostname, session) -> true);
    }

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{new X509TrustManager() {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[]{};
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
        }
    }};
    //------------------------------------------ trust-https end ---------------------------------------------------


    /**
     * post方法调度远程api
     *
     * @param url               远程url
     * @param accessToken       访问令牌
     * @param timeout           超时时间（秒）
     * @param param             请求实体
     * @param returnArgClassOfT 特定的 returnArgClassOfT
     * @return 特定的 ReturnT<returnTargClassOfT> 的Object
     * @see ReturnT
     */
    public static ReturnT postBody(String url, String accessToken, int timeout, Object param, Class returnArgClassOfT) {
        HttpURLConnection connection = null;
        try {
            // connection
            URL realUrl = new URL(url);
            connection = (HttpURLConnection) realUrl.openConnection();

            // 信任https
            boolean useHttps = url.startsWith("https");
            if (useHttps) {
                HttpsURLConnection https = (HttpsURLConnection) connection;
                trustAllHosts(https);
            }

            // connection设置
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setReadTimeout(timeout * 1000);
            connection.setConnectTimeout(3 * 1000);
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            connection.setRequestProperty("Accept-Charset", "application/json;charset=UTF-8");

            if (StringUtils.isNotBlank(accessToken)) {
                connection.setRequestProperty(XXL_JOB_ACCESS_TOKEN, accessToken);
            }

            // do connection
            connection.connect();

            // 写入请求实体
            // write requestBody
            if (param != null) {
                String requestBody = GsonTool.toJson(param);
                try (DataOutputStream dataOutputStream = new DataOutputStream(connection.getOutputStream())) {
                    dataOutputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
                    dataOutputStream.flush();
                }
            }

            // 验证状态码
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-rpc remoting fail, StatusCode(" + statusCode + ") invalid. for url : " + url);
            }
            //解析返回结果
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                // result
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }

                // parse returnT
                return GsonTool.fromJson(result.toString(), ReturnT.class, returnArgClassOfT);
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-rpc remoting error(" + e.getMessage() + "), for url : " + url);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ReturnT<String>(ReturnT.FAIL_CODE, "xxl-rpc remoting error(" + e.getMessage() + "), for url : " + url);
        } finally {
            try {
                if (connection != null) {
                    connection.disconnect();
                }
            } catch (Exception e2) {
                log.error(e2.getMessage(), e2);
            }
        }
    }

}
