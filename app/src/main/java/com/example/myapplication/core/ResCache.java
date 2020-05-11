package com.example.myapplication.core;
import android.text.TextUtils;

import com.example.myapplication.core.cache.ResFileUtils;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.util.HashMap;
import java.util.Map;

public class ResCache {

    private String mH5Url;
    public Map<String, CheckResponse> mResList;

    public ResCache(String h5Url) {
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

    public boolean hasResData() {
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

    static class CheckResponse {

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
