package com.xxl.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 * 网络工具类
 *
 * @author xuxueli 2017-11-29 17:00:25
 */
@Slf4j
public class NetUtil {
    private NetUtil() {
    }

    /**
     * 找到可用的端口号.
     *
     * @param defaultPort 默认的端口号
     * @return 可用的端口号
     * @throws BindException 端口绑定错误
     */
    public static int findAvailablePort(int defaultPort) throws BindException {
        int portTmp = defaultPort;
        //向上查找
        //最大端口值          
        int maxPort = (2 << 15) - 1;
        while (portTmp < maxPort) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp++;
            }
        }
        //向下查找
        portTmp = defaultPort--;
        while (portTmp > 0) {
            if (!isPortUsed(portTmp)) {
                return portTmp;
            } else {
                portTmp--;
            }
        }
        throw new BindException("no available port.");
    }

    /**
     * 检查端口是否使用.
     *
     * @param port 端口号
     * @return 端口已使用，返回true,否则，false
     */
    private static boolean isPortUsed(int port) {
        boolean used = false;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            used = false;
        } catch (IOException e) {
            used = true;
            log.info(">>>>>>>>>>> xxl-rpc, port[{}] is in use.", port);
        }
        return used;
    }
}
