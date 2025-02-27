// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : MinioService
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils;

import com.huest.codesandbox.config.MinioClientSingleton;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;

@Component
public class MinioUtil {

    // todo ERROR 无法正常读取 配置文件
    //@Value("${minio.bucket.src_code}")
    private String src_bucket = "docker-minio-src";

    //@Value("${minio.bucket.sample}")
    private String io_bucket = "docker-sandbox-sample";

    /**
     * 从指定的 src_code 桶中 get 文件
     *
     * @param srcId        唯一
     * @param absolutePath 文件绝对路径 F://main.cc
     * @return int | 0 fail | 1 success |
     */
    @Deprecated
    public int saveCodeFileFromMinio(String srcId, String absolutePath) {
        try {
            GetObjectResponse getObjectResponse = MinioClientSingleton.getInstance().getObject(
                    GetObjectArgs.builder()
                            .bucket(src_bucket)
                            .object(srcId)
                            .build()
            );

            long re = getObjectResponse.transferTo(new FileOutputStream(absolutePath));
            // 此处返回的是文件大小
            if (re == 0) {
                //System.out.println("[=] Error file size : " + re);
                return 0; // 保存文件失败
            }
        } catch (Exception e) {
            // todo
            return 0;
            //System.err.println("[=] Error saveCodeFileFromMinio : " + e.getMessage());
        }
        return 1; // 保存文件成功
    }

    /**
     * 从指定的 io_bucket 桶中 获取 样例 文件
     * prefix 就是 题号 根据不同题号 区分 该题的测试数据
     * 在文件上传 时 应确保测试数据的 .in .out 名字一一对应
     * 如果该题没有输出 或者输入 也应该至少在输入、输出 数据文件中存在一个字符
     *
     * @param qid        prefix
     * @param proDataPath tmp/judge/judge_?_?_?/pro_data
     * @return int
     */
    public int saveIOFileFromMinio(String qid, String proDataPath) {
        try {
            Iterable<Result<Item>> results = MinioClientSingleton.getInstance().listObjects(
                    ListObjectsArgs.builder()
                            .bucket(io_bucket)
                            .prefix(qid + "/")
                            .build()
            );

            // 遍历结果并输出对象名称
            for (Result<Item> result : results) {
                Item item = result.get();
                //System.out.println("对象名称: " + item.objectName());

                String itemName = item.objectName();
                String loadName = proDataPath + File.separator + item.objectName().split("/")[1];

                long re = MinioClientSingleton.getInstance().getObject(
                        GetObjectArgs.builder()
                                .bucket(io_bucket)
                                .object(itemName)
                                .build()
                ).transferTo(new FileOutputStream(loadName));
                if (re == 0) {
                    System.err.println("[=] Error file size : " + re);
                    return 0; // 保存文件失败
                }
            }
            return 1;
        } catch (Exception e) {
            // todo
            System.err.println("[=] Error saveIOFileFromMinio : " + e.getMessage());
            return 0;
        }
    }
}
