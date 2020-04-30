package com.example.myapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.example.myapplication.core.UnLineResManager;
import com.example.myapplication.core.WebViewPool;
import com.example.myapplication.core.cs.BinderPool;
import com.example.myapplication.core.cs.BusinessAction;


public class ResInitService extends Service {

//    private Binder mBinderPool = new BinderPool.BinderPoolImpl();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        WebViewPool.getInstance().init(getApplication());
        return new BusinessAction();
    }

}


