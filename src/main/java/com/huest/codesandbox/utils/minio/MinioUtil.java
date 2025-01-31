// -*- coding = utf-8 -*-
// @Time : 2025/1/30
// @Author : 1zqqx
// @File : MinioService
// @Software : IntelliJ IDEA

package com.huest.codesandbox.utils.minio;

import com.huest.codesandbox.config.MinioClientSingleton;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
                System.out.println("[=] Error file size : " + re);
                return 0; // 保存文件失败
            }
        } catch (Exception e) {
            // todo
            System.out.println("[=] Error saveCodeFileFromMinio : " + e.getMessage());
            return 0;
        }
        return 1; // 保存文件成功
    }

    /**
     * 从指定的 io_bucket 桶中 获取 样例 文件
     * prefix 就是 题号 根据不同题号 区分 该题的样例
     *
     * @param qid Long
     * @return int
     */
    public int saveIOFileFromMinio(Long qid, String samplePath) {
        try {
            Iterable<Result<Item>> results = MinioClientSingleton.getInstance().listObjects(
                    ListObjectsArgs.builder()
                            .bucket(io_bucket)
                            .prefix(qid.toString())
                            .build()
            );

            // 遍历结果并输出对象名称
            for (Result<Item> result : results) {
                Item item = result.get();
                System.out.println("对象名称: " + item.objectName());
            }

            // todo 获取到所有前缀为 qid 的文件
            return 0;
        } catch (Exception e) {
            // todo
            System.out.println("[=] Error saveIOFileFromMinio : " + e.getMessage());
            return 0;
        }
        //return 1; // 保存文件成功
    }
}
