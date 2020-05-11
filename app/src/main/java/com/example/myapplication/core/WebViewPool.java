package com.example.myapplication.core;


import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.tencent.smtt.sdk.WebView;

import java.util.HashMap;
import java.util.Map;

/**
 * webview 复用池
 */
public class WebViewPool {


    private static Map<String, WebView> mH5WebViewHolder;
    private static volatile WebViewPool instance = null;
    private Context mContext;
    private WebView mAvailableWebView;

    public static WebViewPool getInstance () {
        if (instance == null) {
            synchronized (WebViewPool.class) {
                if (instance == null) {
                    instance = new WebViewPool();
                }
            }
        }
        return instance;
    }

    private WebViewPool () {
        mH5WebViewHolder = new HashMap<>();
    }

    /**
     * Webview 初始化
     */
    public void init (Application context) {
        mContext = context;
        mAvailableWebView = new WebView(context);
    }

    /**
     * 获取webview
     */
    public WebView getWebView (String h5Url) {
        if (mContext == null) {
            throw new IllegalStateException("必须先进行初始化！");
        }
        if (TextUtils.isEmpty(h5Url)) {
            return null;
        }
        synchronized (this) {
            WebView webView = null;
            if (mH5WebViewHolder.containsKey(h5Url)) {
                webView = mH5WebViewHolder.get(h5Url);
            }
            if (webView == null) {
                webView = new WebView(mContext);
                mH5WebViewHolder.put(h5Url, webView);
            }

            if (webView.getParent() != null) {
                return new WebView(mContext);
            }
            mAvailableWebView = new WebView(mContext);
            return webView;
        }
    }

    /**
     * 回收webview ,不解绑
     */
    public void removeWebView (ViewGroup parent, WebView webView) {
        if (parent == null || webView == null) {
            return;
        }
        String url = webView.getUrl();
        webView.loadUrl("");
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.clearCache(true);
        webView.clearHistory();
        parent.removeView(webView);
        synchronized (this) {
            if (mH5WebViewHolder.containsKey(url)) {
                WebView invalidWebView = mH5WebViewHolder.remove(url);
                invalidWebView = null;
            } else {
                webView = null;
            }
        }
    }
}

