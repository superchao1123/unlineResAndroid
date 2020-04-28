package com.example.myapplication.core;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

import java.util.HashMap;
import java.util.Map;

/**
 * 磁盘存储规则
 * 1.时效：每个资源保存一周
 * 2.位置：
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

    public class ResCache {

        String mH5Url;
        Map<String, CheckResponse> mResList;

        ResCache(String h5Url) {
            this.mH5Url = h5Url;
            mResList = new HashMap<>();
        }

        public void addRes (String resUrl, WebResourceResponse response) {
            put(resUrl, new CheckResponse(response, 0));
        }

        public WebResourceResponse getResResponse (String resUrl) {
            CheckResponse checkResponse = get(resUrl);
            if (checkResponse != null && checkResponse.isValidity()) {
                return checkResponse.response;
            }
            return null;
        }

        public void recycle() {
            if (mResList != null) {
                mResList.clear();
                mResList = null;
            }
        }

        public void setResInvalidity (String resUrl) {
            if (TextUtils.isEmpty(resUrl)) {
                return;
            }
            CheckResponse checkResponse = get(resUrl);
            if (checkResponse == null) {
                return;
            }
            checkResponse.setStatus(1);
            ResFileUtils.deleteRes(mH5Url, resUrl);
        }

        boolean hasResData() {
            return mResList != null && !mResList.isEmpty();
        }

        public String getH5Url() {
            return mH5Url;
        }

        private void put (String url, CheckResponse response) {
            mResList.put(ResFileUtils.getResDiskName(url), response);
        }

        private CheckResponse get (String url) {
            return mResList.get(ResFileUtils.getResDiskName(url));
        }

        class CheckResponse {

            WebResourceResponse response;
            int status;

            CheckResponse(WebResourceResponse response, int status) {
                this.response = response;
                this.status = status;
            }

            // 资源是否还在使用
            boolean isValidity() {
                return status == 0;
            }

            public void setStatus(int status) {
                this.status = status;
            }
        }
    }

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

        cacheRes(h5Url);
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
        ResCache resCache = new ResCache(h5Url);
        saveCache(resCache.mH5Url, resCache);
        MyWebViewClient webViewClient = new MyWebViewClient(resCache);
        WebView webView = WebViewPool.getInstance().getWebView();
        webView.loadUrl(h5Url);
        webView.setWebViewClient(webViewClient);
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

    class MyWebViewClient extends WebViewClient {


        private final ResCache resCache;

        MyWebViewClient(ResCache resCache) {
            this.resCache = resCache;
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return loadRes(request);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            ResLoadUtils.getInstance().load(url, resCache);
            return null;
        }

        private WebResourceResponse loadRes(WebResourceRequest request) {
            if (request != null) {
                ResLoadUtils.getInstance().load(request, resCache);
            }
            return null;
        }
    }

    /**
     * 下载资源
     */
    public void loadRes (String h5Url, WebResourceRequest request) {
        if (TextUtils.isEmpty(h5Url) || request == null) {
            return;
        }
        ResLoadUtils.getInstance().load(request, new ResCache(h5Url));
    }
}
