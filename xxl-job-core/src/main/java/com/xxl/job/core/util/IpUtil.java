package com.xxl.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * ip工具类
 *
 * @author xuxueli 2016-5-22 11:38:05
 */
@Slf4j
public class IpUtil {
    private IpUtil() {
    }

    /**
     * 一个无效的，未知的或者不可用的目标.
     */
    private static final String ANY_HOST_VALUE = "0.0.0.0";
    /**
     * 本地地址.
     */
    private static final String LOCALHOST_VALUE = "127.0.0.1";
    /**
     * 地址正则表达式.
     */
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    /**
     * 本地地址.
     */
    private static InetAddress localAddress = null;

    // ---------------------- valid ----------------------

    /**
     * 验证地址
     *
     * @param address 地址
     * @return 验证后规范化的地址
     */
    private static InetAddress toValidAddress(InetAddress address) {
        if (address instanceof Inet6Address) {
            Inet6Address v6Address = (Inet6Address) address;
            if (isPreferIpv6Address()) {
                return normalizeV6Address(v6Address);
            }
        }
        if (isValidV4Address(address)) {
            return address;
        }
        return null;
    }

    /**
     * 判断是否支持ip6地址.
     *
     * @return true, 支持.不支持，false
     */
    private static boolean isPreferIpv6Address() {
        return Boolean.getBoolean("java.net.preferIPv6Addresses");
    }

    /**
     * 验证 Inet4地址
     *
     * @param address 验证的地址
     * @return true, 为Inet4地址.否则，false
     */
    private static boolean isValidV4Address(InetAddress address) {
        //  当IP地址是loopback地址时返回true，否则返回false.
        //  loopback地址就是代表本机的IP地址。IPv4的loopback地址的范围是127.0.0.0 ~ 127.255.255.255，也就是说，只要第一个字节是127，就是loopback地址。如127.1.2.3、127.0.200.200都是loopback地址。
        //  IPv6的loopback地址是0：0：0：0：0：0：0：1，也可以简写成：：1.
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return !(name == null || !IP_PATTERN.matcher(name).matches() || ANY_HOST_VALUE.equals(name) || LOCALHOST_VALUE.equals(name));
    }


    /**
     * 规范化ipv6地址，将作用域名称转换为作用域id.
     * <p>
     * 例如: 转换</p>
     * <pre>
     * fe80:0:0:0:894:aeec:f37d:23e1%en0 -> fe80:0:0:0:894:aeec:f37d:23e1%5</pre>
     * <p>
     * ipv6地址后的%5称为作用域id。有关详细信息，请参阅{@link Inet6Address}的java文档
     *
     * @param address 输入地址ccc
     * @return 使用范围ID转换为int的规范化地址
     */
    private static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException e) {
                // ignore
                log.debug("Unknown IPV6 address: ", e);
            }
        }
        return address;
    }

    // ---------------------- find ip ----------------------

    /**
     * 获取本地地址.
     *
     * @return 本地地址
     */
    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            //找到本地地址，不为空直接返回
            localAddress = InetAddress.getLocalHost();
            InetAddress addressItem = toValidAddress(localAddress);
            if (addressItem != null) {
                return addressItem;
            }


            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }
            //存在更多的网卡接口
            while (interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement();
                //验证是否有效的ip
                if (network.isLoopback() || network.isVirtual() || !network.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = network.getInetAddresses();
                while (addresses.hasMoreElements()) {

                    addressItem = toValidAddress(addresses.nextElement());
                    //测试该地址是否可访问,超时时间为100ms
                    if (addressItem != null && addressItem.isReachable(100)) {
                        return addressItem;
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return localAddress;
    }


    // ---------------------- tool ----------------------

    /**
     * 从本地网卡查找第一个有效IP
     *
     * @return 第一个有效IP
     */
    private static InetAddress getLocalAddress() {
        if (localAddress != null) {
            return localAddress;
        }
        localAddress = getLocalAddress0();
        return localAddress;
    }

    /**
     * 获取本地ip地址.
     *
     * @return 本地地址字符串
     */
    public static String getIp() {
        InetAddress hostAddress = getLocalAddress();
        if (hostAddress != null) {
            return hostAddress.getHostAddress();
        }
        return null;
    }

    /**
     * 获取ip+端口
     *
     * @param port 端口号
     * @return ip+端口号
     */
    public static String getIpPort(int port) {
        String ip = getIp();
        return getIpPort(ip, port);
    }

    /**
     * 获取ip+端口
     *
     * @param ip   ip地址
     * @param port 端口号
     * @return ip+端口号
     */
    public static String getIpPort(String ip, int port) {
        if (ip == null) {
            return null;
        }
        return ip.concat(":").concat(String.valueOf(port));
    }

    /**
     * 解析地址.
     *
     * @param address 地址
     * @return 返回对角数组，obj[0]为ip,ojb[1]为port
     */
    public static Object[] parseIpPort(String address) {
        String[] array = address.split(":");

        String host = array[0];
        int port = Integer.parseInt(array[1]);

        return new Object[]{host, port};
    }


}
