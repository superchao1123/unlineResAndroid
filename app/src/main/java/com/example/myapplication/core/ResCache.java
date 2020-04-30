package com.example.myapplication.core;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.example.myapplication.core.cache.ResFileUtils;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.util.HashMap;
import java.util.Map;

public class ResCache implements Parcelable {

    private String mH5Url;
    public Map<String, CheckResponse> mResList;

    public ResCache(String h5Url) {
        this.mH5Url = h5Url;
        mResList = new HashMap<>();
    }

    protected ResCache(Parcel in) {
        mH5Url = in.readString();
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mH5Url);
    }

    public static final Creator<ResCache> CREATOR = new Creator<ResCache>() {
        @Override
        public ResCache createFromParcel(Parcel in) {
            return new ResCache(in);
        }

        @Override
        public ResCache[] newArray(int size) {
            return new ResCache[size];
        }
    };

    static class CheckResponse implements Parcelable {

        WebResourceResponse response;
        int status;

        CheckResponse(WebResourceResponse response, int status) {
            this.response = response;
            this.status = status;
        }

        protected CheckResponse(Parcel in) {
            status = in.readInt();
        }

        public static final Creator<CheckResponse> CREATOR = new Creator<CheckResponse>() {
            @Override
            public CheckResponse createFromParcel(Parcel in) {
                return new CheckResponse(in);
            }

            @Override
            public CheckResponse[] newArray(int size) {
                return new CheckResponse[size];
            }
        };

        // 资源是否还在使用
        boolean isValidity() {
            return status == 0;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(status);
        }
    }
}
