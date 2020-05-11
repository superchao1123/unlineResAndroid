package com.example.myapplication.core.cs;

import android.os.Parcel;
import android.os.Parcelable;

public class ResEntity implements Parcelable {

    private String h5Url;
    private String resUrl;
    private String dirPath;

    protected ResEntity(Parcel in) {
        h5Url = in.readString();
        resUrl = in.readString();
        dirPath = in.readString();
    }

    public static final Creator<ResEntity> CREATOR = new Creator<ResEntity>() {
        @Override
        public ResEntity createFromParcel(Parcel in) {
            return new ResEntity(in);
        }

        @Override
        public ResEntity[] newArray(int size) {
            return new ResEntity[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(h5Url);
        dest.writeString(resUrl);
        dest.writeString(dirPath);
    }

    /** 从Parcel中读取数据 **/
    public void readFromParcel(Parcel in){
        h5Url = in.readString();
        resUrl = in.readString();
        resUrl = in.readString();
    }
}
