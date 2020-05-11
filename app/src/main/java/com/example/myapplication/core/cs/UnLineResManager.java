package com.example.myapplication.core.cs;

import android.text.TextUtils;

import com.example.myapplication.core.cache.ResFileUtils;
import com.example.myapplication.core.download.DownloadCallback;
import com.example.myapplication.core.download.DownloadManager;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * todo
 * 1、数据库存储？
 * 2、资源文件时效
 * 3、单进程 + ipc 策略实现
 *    预热资源 下载资源到文件 完毕回调主进程 读取磁盘资源到内存
 * 4、dif资源下载：简易后端搭建、可行性测试、cdn、。。。
 *
 */
public class UnLineResManager {

    /**
     * 预热h5资源
     * 调用时机：h5可被打开状态
     * 1.磁盘没有资源，下载并读到内存
     * 2.内存没有资源，磁盘读到内存
     */
    static void preStart (String h5Url) {

        // 检查资源磁盘是否存在，存在读到内存
        boolean cached = checkResCacheDisk(h5Url);

        // 下载资源
        if (!cached) {
            loadRes(h5Url);
        }
    }

    private static boolean checkResCacheDisk(String h5Url) {
        return ResFileUtils.isResCacheDisk(h5Url);
    }

    /**
     * 下载资源
     */
    static void loadRes(final String h5Url, String resUrl) {
        ArrayList<String> resUrls = new ArrayList<>();
        resUrls.add(resUrl);
        DownloadManager.getInstance().load(resUrls, "", "", new DownloadCallback.SimpleDownloadCallback() {
            @Override
            public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
                enjoyResponse(h5Url, resUrl, content, rspHeaders);
            }
        });
    }

    /**
     * 下载资源
     */
    static void loadRes(final String h5Url, List<String> resUrls) {
        DownloadManager.getInstance().load(resUrls, "", "", new DownloadCallback.SimpleDownloadCallback() {
            @Override
            public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
                enjoyResponse(h5Url, resUrl, content, rspHeaders);
            }
        });
    }

    /**
     * 下载资源
     */
    static void loadRes(final String h5Url) {
        if (TextUtils.isEmpty(h5Url)) {
            return;
        }
        getNeedPreStartResLinks(new GetPreStartLinkCallback() {
            @Override
            public void finished(final List<String> links) {
                if (links != null && !links.isEmpty()) {
                    DownloadManager.getInstance().load(links, "192.168.0.106", "", new DownloadCallback.SimpleDownloadCallback() {
                        @Override
                        public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
                            enjoyResponse(h5Url, resUrl, content, rspHeaders);
                        }
                    });
                }
            }
        });
    }

    private static void enjoyResponse (String h5Url, String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
        ResFileUtils.writeFile(ResFileUtils.convertHeadersToString(rspHeaders), ResFileUtils.getResResponseHeaderPath(h5Url, resUrl));
        ResFileUtils.writeToDisk(resUrl, new ByteArrayInputStream(content), h5Url);
    }

    interface GetPreStartLinkCallback {
        void finished(List<String> links);
    }

    // TODO: 2020-04-29 获取资源列表
    private static void getNeedPreStartResLinks(GetPreStartLinkCallback callback) {
        ArrayList<String> list = new ArrayList<>();
        list.add("http://192.168.0.106:8080/htmldemo/123.html");
        list.add("http://192.168.0.106:8080/htmldemo/jQuery/jQuery.js");
        callback.finished(list);
    }
}
