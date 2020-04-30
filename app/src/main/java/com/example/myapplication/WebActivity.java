package com.example.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.example.myapplication.core.UnLineResManager;
import com.example.myapplication.core.WebViewPool;
import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;

public class WebActivity extends Activity {

    private ViewGroup mGroup;
    private WebView mWebView;

    public static void start(Context context) {
        Intent intent = new Intent(context, WebActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        mGroup = findViewById(R.id.v_group);
        mWebView = WebViewPool.getInstance().getWebView(Config.TEST_URL);
        mWebView.loadUrl(Config.TEST_URL);
        mWebView.setWebViewClient(new MyWebViewClient());
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        mGroup.addView(mWebView, layoutParams);
    }

    class MyWebViewClient extends WebViewClient {

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView webView, WebResourceRequest request) {
            WebResourceResponse webResourceResponse = UnLineResManager.getInstance().getRes(Config.TEST_URL, request.getUrl().toString());
            if (webResourceResponse == null) {
                UnLineResManager.getInstance().loadRes(Config.TEST_URL, request);
                webResourceResponse = super.shouldInterceptRequest(webView, request);
            }
            return webResourceResponse;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebViewPool.getInstance().removeWebView(mGroup, mWebView);
    }
}
