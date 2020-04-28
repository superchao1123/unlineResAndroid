package com.example.myapplication.core;

import android.text.TextUtils;

import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

class WebResponseTranslator {

    static WebResourceResponse transform(String resUrl, Response response) {
        if (TextUtils.isEmpty(resUrl) || response == null) {
            return null;
        }
        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }
        return transform(resUrl, body.byteStream(), response.headers());
    }

    static WebResourceResponse transform(String resUrl, FileInputStream inputStream) {
        if (TextUtils.isEmpty(resUrl) || inputStream == null) {
            return null;
        }
        return transform(resUrl, inputStream, null);
    }

    private static WebResourceResponse transform (String resUrl, InputStream inputStream, Headers headers) {
        try {
            WebResourceResponse webResourceResponse = null;
            String mimeType;
            if (resUrl.contains(".js")) {
                mimeType = "text/javascript";
            } else {
                mimeType = MimeTypeMapUtils.getMimeTypeFromUrl(resUrl);
            }
            if (!TextUtils.isEmpty(mimeType)) {
                webResourceResponse = new WebResourceResponse(mimeType, "UTF-8", inputStream);
                if (headers != null) {
                    HashMap<String, String> map = new HashMap<>();
                    for (String key : headers.names()) {
                        map.put(key, headers.get(key));
                    }
                    webResourceResponse.setResponseHeaders(map);
                }
                return webResourceResponse;
            }
        } catch (Exception e) {

        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {}
        }
        return null;
    }
}
