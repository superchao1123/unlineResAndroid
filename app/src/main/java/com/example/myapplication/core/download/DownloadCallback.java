package com.example.myapplication.core.download;

import java.util.List;
import java.util.Map;

public interface DownloadCallback {

    void onStart();

    void onProgress(int pro, int total);

    void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders);

    void onError(int errorCode);

    void onFinish();

    class SimpleDownloadCallback implements DownloadCallback {

        @Override
        public void onStart() { }

        @Override
        public void onProgress(int pro, int total) { }

        @Override
        public void onSuccess(String resUrl, byte[] content, Map<String, List<String>> rspHeaders) { }

        @Override
        public void onError(int errorCode) { }

        @Override
        public void onFinish() { }
    }
}
