package com.example.myapplication.core;

import com.tencent.smtt.export.external.interfaces.WebResourceRequest;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class ResLoadUtils {

    private static volatile ResLoadUtils instance;
    private final OkHttpClient mOkHttpClient;

    static ResLoadUtils getInstance() {
        if (instance == null) {
            synchronized (ResLoadUtils.class) {
                if (instance == null) {
                    instance = new ResLoadUtils();
                }
            }
        }
        return instance;
    }

    private ResLoadUtils () {
        mOkHttpClient = new OkHttpClient();
    }

    void load(WebResourceRequest webRequest, UnLineResManager.ResCache resCache) {
        if (webRequest == null) {
            return;
        }
        Map<String, String> headers = webRequest.getRequestHeaders();
        Request.Builder builder = new Request.Builder().url(webRequest.getUrl().toString()).get();
        if (headers != null) {
            builder.headers(Headers.of(headers))
                    .addHeader("Connection", "close");
        }
        Request request = builder.build();
        load(request, resCache);
    }

    void load(String url, UnLineResManager.ResCache resCache) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();
        load(request, resCache);
    }

    private void load(@NotNull Request request, final UnLineResManager.ResCache resCache) {
        final long startTime = System.currentTimeMillis();
        final String resUrl = request.url().toString();
        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                ResLogUtils.log("download " + resUrl + " failed");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                saveToHeap(resUrl, response, resCache);
                ResFileUtils.writeToDisk(startTime, resUrl, response, resCache);
            }
        });
    }

    private void saveToHeap(String resUrl, Response response, UnLineResManager.ResCache resCache) {
        WebResourceResponse webResourceResponse = WebResponseTranslator.transform(resUrl, response);
        resCache.addRes(resUrl, webResourceResponse);
    }
}
