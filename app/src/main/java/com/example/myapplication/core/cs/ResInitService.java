package com.example.myapplication.core.cs;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.Nullable;

public class ResInitService extends Service {

    public static Context mContext;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mContext = this;
        return new BusinessAction();
    }

    public static class BusinessAction extends IBusinessAction.Stub {

        @Override
        public void preStart(String h5url) throws RemoteException {
            UnLineResManager.preStart(h5url);
        }

        @Override
        public void loadAllRes(String h5url) throws RemoteException {
            UnLineResManager.loadRes(h5url);
        }

        @Override
        public void loadRes(String h5url, String resUrl) throws RemoteException {
            UnLineResManager.loadRes(h5url, resUrl);
        }

        @Override
        public ResEntity getRes(String h5url, String resUrl) throws RemoteException {
            return null;
        }

        @Override
        public int getPid() throws RemoteException {
            return android.os.Process.myPid();
        }

        /**此处可用于权限拦截**/
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return super.onTransact(code, data, reply, flags);
        }
    }
}


