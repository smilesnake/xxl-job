package com.xxl.job.admin.core.route.strategy;

import com.xxl.job.admin.core.route.ExecutorRouter;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.remote.TriggerParam;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 一致性HASH：每个任务按照Hash算法固定选择某一台机器，且所有任务均匀散列在不同机器上
 * <p>分组下机器地址相同，不同JOB均匀散列在不同机器上，保证分组下机器分配JOB平均；且每个JOB固定调度其中一台机器；</p>
 * <ol>
 *     <li> virtual node：解决不均衡问题</li>
 *     <li>hash method replace hashCode：String的hashCode可能重复，需要进一步扩大hashCode的取值范围</li>
 * </ol>
 *
 * @author xuxueli on 17/3/10.
 */
public class ExecutorRouteConsistentHash extends ExecutorRouter {
    /**
     * 虚拟的节点数.
     */
    private static int VIRTUAL_NODE_NUM = 100;

    /**
     * get hash code on 2^32 ring (md5散列的方式计算hash值)
     *
     * @param key
     * @return
     */
    private static long hash(String key) {
        //截取的hashCode
        long truncateHashCode;
        try {
            // md5 byte
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            // 重置
            md5.reset();
            byte[] keyBytes = key.getBytes("UTF-8");
            md5.update(keyBytes);

            // jobId转换为md5
            // 不直接用hashCode() 是因为扩大hash取值范围，减少冲突
            byte[] digest = md5.digest();

            // hash code, Truncate to 32-bits
            // 32位hashCode
            long hashCode = ((long) (digest[3] & 0xFF) << 24)
                    | ((long) (digest[2] & 0xFF) << 16)
                    | ((long) (digest[1] & 0xFF) << 8)
                    | (digest[0] & 0xFF);
            truncateHashCode = hashCode & 0xffffffffL;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + key, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        return truncateHashCode;
    }

    /**
     * hash任务
     *
     * @param jobId       任务id
     * @param addressList 地址列表
     * @return
     */
    public String hashJob(int jobId, List<String> addressList) {

        // ------A1------A2-------A3------
        // -----------J1------------------
        // 地址哈希环
        TreeMap<Long, String> addressRing = new TreeMap<>();
        for (String address : addressList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                // 通过自定义的Hash方法，得到服务节点的Hash值，同时放入treeMap
                long addressHash = hash("SHARD-" + address + "-NODE-" + i);
                addressRing.put(addressHash, address);
            }
        }

        // 得到JobId的Hash值
        long jobHash = hash(String.valueOf(jobId));
        // 调用treeMap的tailMap方法，拿到map中键大于jobHash的值列表
        SortedMap<Long, String> lastRing = addressRing.tailMap(jobHash);
        // 如果addressRing中有比jobHash的那么直接取lastRing的第一个
        if (!lastRing.isEmpty()) {
            return lastRing.get(lastRing.firstKey());
        }
        // 如果没有，则直接取addresRing的第一个
        // 反正最终的效果是在Hash环上，顺时针拿离jobHash最近的一个值
        return addressRing.firstEntry().getValue();
    }

    @Override
    public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {
        String address = hashJob(triggerParam.getJobId(), addressList);
        return new ReturnT<>(address);
    }

}
