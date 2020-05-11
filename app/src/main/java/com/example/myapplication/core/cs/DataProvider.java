package com.example.myapplication.core.cs;

import android.content.Intent;
import android.text.TextUtils;

import com.example.myapplication.core.ResCache;
import com.example.myapplication.core.WebResponseTranslator;
import com.example.myapplication.core.cache.ResFileUtils;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 主进程数据输出
 */
public class DataProvider {

    private Map<String, ResCache> mCacheData;

    private static volatile DataProvider instance;

    static DataProvider getInstance() {
        if (instance == null) {
            synchronized (DataProvider.class) {
                if (instance == null) {
                    instance = new DataProvider();
                }
            }
        }
        return instance;
    }

    private DataProvider () {
        mCacheData = new HashMap<>();
    }

    /**
     * 获取资源
     * 不存在时返回null，由h5自己拉取
     */
    WebResourceResponse getRes (String h5Url, String resUrl) {
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
    void recycle (String h5Url) {
        if (TextUtils.isEmpty(h5Url) || mCacheData == null || mCacheData.isEmpty()) {
            return;
        }

        ResCache remove = mCacheData.remove(h5Url);
        if (remove != null) {
            remove.recycle();
            remove = null;
        }
        System.gc();
    }

    /**
     * 释放资源占用内存
     */
    void recycle (List<String> h5Urls) {

        if (null == h5Urls || h5Urls.isEmpty() || mCacheData == null || mCacheData.isEmpty()) {
            return;
        }

        for (String h5Url : h5Urls) {
            if (TextUtils.isEmpty(h5Url)) continue;
            ResCache remove = mCacheData.remove(h5Url);
            if (remove != null) {
                remove.recycle();
                remove = null;
            }
        }

        System.gc();
    }

    /**
     * 释放占用内存
     */
    void recycle () {
        if (mCacheData != null) {
            mCacheData.clear();
            System.gc();
        }
    }

    boolean checkCache(String h5Url) {
        return mCacheData != null && mCacheData.containsKey(h5Url);
    }

    void saveCache (Intent data) {
        String h5Url = data.getStringExtra("h5Url");
        String resDirPath = data.getStringExtra("resDirPath");
        ResCache resCache = null;
        if (mCacheData.containsKey(h5Url)) {
            resCache = mCacheData.get(h5Url);
        }
        if (resCache == null) {
            resCache = new ResCache(h5Url);
            mCacheData.put(h5Url, resCache);
        }

        if (TextUtils.isEmpty(resDirPath)) {
            return;
        }

        File file = new File(resDirPath);
        if (!file.exists() || !file.isDirectory()) {
           return;
        }

        File[] files = file.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (File resFile : files) {
            if (resFile == null || !resFile.exists() || !resFile.isFile() || resFile.getName().contains(ResFileUtils.HEADER_EXT)) {
                continue;
            }
            try {
                String fileName = resFile.getName();
                WebResourceResponse response = WebResponseTranslator.transform(fileName,
                        ResFileUtils.readFileToBytes(resFile),
                        ResFileUtils.getHeaderFromLocalCache(
                                ResFileUtils.getResResponseHeaderPath(resCache.getH5Url(), fileName)));
                resCache.addRes(fileName, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
