package com.example.myapplication.core;

import android.text.TextUtils;

import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class WebResponseTranslator {

    public static WebResourceResponse transform(String resUrl, Response response) {
        if (TextUtils.isEmpty(resUrl) || response == null) {
            return null;
        }
        ResponseBody body = response.body();
        if (body == null) {
            return null;
        }
        return transform(resUrl, body.byteStream(), response.headers());
    }

    public static WebResourceResponse transform(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) {
        if (TextUtils.isEmpty(resUrl) || content == null) {
            return null;
        }
        Headers.Builder builder = null;
        if (rspHeaders != null && !rspHeaders.isEmpty()) {
            builder = new Headers.Builder();
            Set<Map.Entry<String, List<String>>> entries =  rspHeaders.entrySet();
            for (Map.Entry<String, List<String>> entry : entries) {
                String key = entry.getKey();
                if (TextUtils.isEmpty(key)) {
                    continue;
                }
                List<String> values = entry.getValue();
                if (values == null || values.isEmpty()) {
                    continue;
                }
                StringBuilder headerString = new StringBuilder();
                for (String value : values) {
                    if (TextUtils.isEmpty(value)) {
                        continue;
                    }
                    if (headerString.length() != 0) {
                        headerString.append(";");
                    }
                    headerString.append(value);
                }
                if (TextUtils.isEmpty(headerString.toString())) {
                    continue;
                }
                builder.add(key, headerString.toString());
            }
        }
        InputStream inputStream = new ByteArrayInputStream(content);
        return transform(resUrl, inputStream, builder == null ? null : builder.build());
    }

    public static WebResourceResponse transform (String resUrl, InputStream inputStream, Headers headers) {
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
