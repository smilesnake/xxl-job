package com.xxl.job.core.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

/**
 * 文件工具类
 *
 * @author xuxueli 2017-12-29 17:56:48
 */
@Slf4j
public class FileUtil {
    private FileUtil() {
    }

    /**
     * 递归删除文件.
     *
     * @param root 根文件目录
     */
    public static void deleteRecursively(File root) {
        if (root != null && root.exists()) {
            if (root.isDirectory()) {
                File[] children = root.listFiles();
                if (children != null) {
                    for (File child : children) {
                        deleteRecursively(child);
                    }
                }
            }
            try {
                Files.delete(root.toPath());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param fileName 文件名
     */
    public static void deleteFile(String fileName) throws IOException {
        // file
        File file = new File(fileName);
        if (file.exists()) {
            Files.delete(file.toPath());
        }
    }

    /**
     * 写入文件内容
     *
     * @param file 待写入的文件
     * @param data 待写入的数据
     */
    public static void writeFileContent(File file, byte[] data) {

        // file
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }
        // append file content
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
            fos.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 读取文件内容.
     *
     * @param file 待读取的文件
     * @return 读取的文件字节数
     */
    public static byte[] readFileContent(File file) {
        Long length = file.length();
        byte[] fileContent = new byte[length.intValue()];

        try (FileInputStream in = new FileInputStream(file)) {
            int len = in.read(fileContent);
            while (len != -1) {
                len = in.read(fileContent);
            }
            return fileContent;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
