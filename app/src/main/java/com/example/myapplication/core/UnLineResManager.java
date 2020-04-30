package com.example.myapplication.core;

import android.text.TextUtils;

import com.example.myapplication.core.cache.ResFileUtils;
import com.example.myapplication.core.download.DownloadCallback;
import com.example.myapplication.core.download.DownloadManager;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static volatile UnLineResManager instance;

    public static UnLineResManager getInstance() {
        if (instance == null) {
            synchronized (UnLineResManager.class) {
                if (instance == null) {
                    instance = new UnLineResManager();
                }
            }
        }
        return instance;
    }

    private UnLineResManager () {
        mCacheData = new HashMap<>();
    }

    // todo lru保存数据
    private Map<String, ResCache> mCacheData;

    /**
     * 预热h5资源
     * 调用时机：h5可被打开状态
     * 1.磁盘没有资源，下载并读到内存
     * 2.内存没有资源，磁盘读到内存
     */
    public void preStart (String h5Url) {
        if (TextUtils.isEmpty(h5Url)) {
            return;
        }

        // 检查资源内存是否存在
        boolean cached = checkResCacheHeap(h5Url);

        // 检查资源磁盘是否存在，存在读到内存
        if (!cached) {
            cached = checkResCacheDisk(h5Url);
        }

        // 下载资源
        if (!cached) {
            cacheRes(h5Url);
        }
    }

    /**
     * 获取资源
     * 不存在时返回null，由h5自己拉取
     */
    public WebResourceResponse getRes (String h5Url, String resUrl) {
        if (TextUtils.isEmpty(h5Url) || TextUtils.isEmpty(resUrl) || mCacheData == null) {
            return null;
        }
        ResCache resCache = mCacheData.get(h5Url);
        if (resCache ==  null) {
            return null;
        }
        return resCache.getResResponse(resUrl);
    }

    /**
     * 释放资源占用内存
     */
    public void recycle (String h5Url) {
        if (TextUtils.isEmpty(h5Url)) {
            return;
        }

        if (mCacheData != null) {
            ResCache remove = mCacheData.remove(h5Url);
            if (remove != null) {
                remove.recycle();
                remove = null;
            }
            System.gc();
        }
    }

    private void cacheRes(String h5Url) {
        final ResCache resCache = new ResCache(h5Url);
        saveCache(resCache.getH5Url(), resCache);
        getNeedPreStartResLinks(new GetPreStartLinkCallback() {
            @Override
            public void finished(final List<String> links) {
                if (links != null && !links.isEmpty()) {
                    DownloadManager.getInstance().load(links, "", "", new DownloadCallback.SimpleDownloadCallback() {
                        @Override
                        public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
                            enjoyResponse(resCache, resUrl, content, rspHeaders);
                        }
                    });
                }
            }
        });
    }

    private void enjoyResponse (ResCache resCache, String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
        // 保存到内存
        WebResourceResponse webResourceResponse = WebResponseTranslator.transform(resUrl, content, rspHeaders);
        resCache.addRes(resUrl, webResourceResponse);
        // 保存到磁盘
        ResFileUtils.writeFile(ResFileUtils.convertHeadersToString(rspHeaders), ResFileUtils.getResResponseHeaderPath(resCache.getH5Url(), resUrl));
        ResFileUtils.writeToDisk(resUrl, new ByteArrayInputStream(content), resCache);
    }

    interface GetPreStartLinkCallback {
        void finished(List<String> links);
    }

    // TODO: 2020-04-29 获取资源列表
    private void getNeedPreStartResLinks(GetPreStartLinkCallback callback) {
        ArrayList<String> list = new ArrayList<>();
        list.add("http://10.155.31.83:8080/htmldemo/123.html");
        list.add("http://10.155.31.83:8080/htmldemo/jQuery/jQuery.js");
        callback.finished(list);
    }

    private boolean checkResCacheHeap (String h5Url) {
        ResCache resCache = mCacheData.get(h5Url);
        return mCacheData != null
                && mCacheData.containsKey(h5Url)
                && resCache != null
                && resCache.hasResData();
    }

    private void saveCache (String h5Url, ResCache resCache) {
        if (mCacheData != null && !mCacheData.containsKey(h5Url)) {
            mCacheData.put(h5Url, resCache);
        }
    }

    private boolean checkResCacheDisk(String h5Url) {
        ResCache resCache = new ResCache(h5Url);
        boolean isResCacheDisk = ResFileUtils.isResCacheDisk(resCache);
        if (isResCacheDisk) {
            mCacheData.put(h5Url, resCache);
        } else {
            resCache.recycle();
            resCache = null;
        }
        return isResCacheDisk;
    }

    /**
     * 下载资源
     */
    public void loadRes (final String h5Url, WebResourceRequest request) {
        if (TextUtils.isEmpty(h5Url) || request == null) {
            return;
        }
        DownloadManager.getInstance().load(request.getUrl().toString(), "", "", new DownloadCallback.SimpleDownloadCallback() {
            @Override
            public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
                ResCache resCache = mCacheData.get(h5Url);
                if (resCache == null) {
                    resCache = new ResCache(h5Url);
                    saveCache(h5Url, resCache);
                }
                enjoyResponse(resCache, resUrl, content, rspHeaders);
            }
        });
    }
}
