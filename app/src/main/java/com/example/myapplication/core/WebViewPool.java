package com.example.myapplication.core;


import android.app.Application;
import android.content.Context;
import android.view.ViewGroup;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.util.ArrayList;
import java.util.List;

/**
 * webview 复用池
 */
public class WebViewPool {

    private static final String APP_CACAHE_DIRNAME = "webCache";
    private static List<WebView> available = new ArrayList<>();
    private static List<WebView> inUse = new ArrayList<>();
    private static int maxSize = 2;
    private int currentSize = 0;
    private static long startTimes = 0;
    private static volatile WebViewPool instance = null;
    private static Context mContext;

    public static WebViewPool getInstance() {
        if (instance == null) {
            synchronized (WebViewPool.class) {
                if (instance == null) {
                    instance = new WebViewPool();
                }
            }
        }
        return instance;
    }

    private WebViewPool() {

    }

    /**
     * Webview 初始化
     * 最好放在application oncreate里
     */
    public static void init(Application context) {
        mContext = context;
        for (int i = 0; i < maxSize; i++) {
            WebView webView = new WebView(context);
            initWebSetting(webView);
            available.add(webView);
        }
    }

    /**
     * 获取webview
     */
    public WebView getWebView() {
        synchronized (this) {
            WebView webView;
            if (available.size() > 0) {
                webView = available.get(0);
                available.remove(0);
                currentSize++;
                inUse.add(webView);
            } else {
                webView = new WebView(mContext);
                initWebSetting(webView);
                inUse.add(webView);
                currentSize++;
            }
            return webView;
        }
    }

    /**
     * 回收webview ,不解绑
     *
     * @param webView 需要被回收的webview
     */
    public void removeWebView(WebView webView) {
        webView.loadUrl("");
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.clearCache(true);
        webView.clearHistory();
        synchronized (this) {
            inUse.remove(webView);
            if (available.size() < maxSize) {
                available.add(webView);
            } else {
                webView = null;
            }
            currentSize--;
        }
    }

    /**
     * 回收webview ,解绑
     *
     * @param webView 需要被回收的webview
     */
    public void removeWebView(ViewGroup viewGroup, WebView webView) {
        viewGroup.removeView(webView);
        webView.loadUrl("");
        webView.stopLoading();
        webView.setWebChromeClient(null);
        webView.setWebViewClient(null);
        webView.clearCache(true);
        webView.clearHistory();
        synchronized (this) {
            inUse.remove(webView);
            if (available.size() < maxSize) {
                available.add(webView);
            } else {
                webView = null;
            }
            currentSize--;
        }
    }

    /**
     * 设置webview池个数
     *
     * @param size webview池个数
     */
    public void setMaxPoolSize(int size) {
        synchronized (this) {
            maxSize = size;
        }
    }

    private static void initWebSetting(WebView webView) {
        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        mWebSettings.setAllowFileAccess(false);
        mWebSettings.setPluginState(WebSettings.PluginState.ON);
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        mWebSettings.setSavePassword(false);
        mWebSettings.setSaveFormData(true);
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setDatabaseEnabled(true);
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        mWebSettings.setUseWideViewPort(true); // 将图片调整到适合webview的大小
        mWebSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小
        mWebSettings.setMediaPlaybackRequiresUserGesture(false);// 允许H5的video标签自动播放
//        String mKooUA = DeviceInfoUtil.getWebViewUA(mWebSettings.getUserAgentString());
//        mWebSettings.setUserAgentString(mKooUA);
    }
}

