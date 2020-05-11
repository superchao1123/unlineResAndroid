package com.example.myapplication.core.cs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentCallbacks;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.myapplication.core.ResLogUtils;
import com.example.myapplication.core.WebViewPool;
import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.util.List;

/**
 * 跨进程
 */
public class DataRes {

    public static final String MSG_TYPE_PERFORM_READ_DATA = "msg_type_perform_read_data";
    @SuppressLint("StaticFieldLeak")
    private static DataRes instance;
    private static boolean mIsInit;
    private Context mContext;
    private IBusinessAction iBusinessAction;
    private DataServiceConnection mConnection;
    private boolean mIsBound;

    public static void init (Activity mainActivity) {
        if (null == mainActivity || mainActivity.isFinishing() || mIsInit) return;
        mIsInit = true;
        instance = new DataRes();
        instance.mContext = mainActivity;
        // 开启获取资源数据进程
        instance.startDataProcess();
        // 注册跨进程接收广播
        instance.registerListener();
        // 初始化web池
        Application application = mainActivity.getApplication();
        if (application == null) return;
        WebViewPool.getInstance().init(application);
        // 监听app内存
        instance.watchMainAppMemory(application);
        // 监听挂接页面生命周期
        instance.hookLifecycle(application, mainActivity);
        ResLogUtils.log("init finish");
    }

    private void watchMainAppMemory(Application application) {
        application.registerComponentCallbacks(new ComponentCallbacks() {
            @Override
            public void onConfigurationChanged(@NonNull Configuration newConfig) {

            }

            @Override
            public void onLowMemory() {
                ResLogUtils.log("onLowMemory");
                DataProvider.getInstance().recycle();
            }
        });
    }

    private void hookLifecycle(Application application, final Activity mainActivity) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                if (mainActivity.equals(activity)) {
                    ResLogUtils.log("mainActivityDestroyed");
                    mainActivityDestroyed();
                }
            }
        });
    }

    private void registerListener() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MSG_TYPE_PERFORM_READ_DATA);
        mContext.registerReceiver(new DataBroadcastReceiver(), intentFilter);
    }

    class DataBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MSG_TYPE_PERFORM_READ_DATA.equals(intent.getAction())) {
                DataProvider.getInstance().saveCache(intent);
            }
        }
    }

    /**
     * 绑定远程服务
     */
    private void startDataProcess () {
        if (null == mContext) return;
        ResLogUtils.log("bindRemoteService");
        mConnection = new DataServiceConnection();
        Intent intent = new Intent();
        intent.setClass(mContext, ResInitService.class);
        mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    /**
     * 解除绑定远程服务
     */
    private void unbindRemoteService () {
        if (null == mContext || !mIsBound) return;
        ResLogUtils.log("unbindRemoteService");
        mContext.unbindService(mConnection);
        mIsBound = false;
        mIsInit = false;
        instance = null;
    }

    /**
     * 杀死远程服务
     */
    private void killRemoteService () {
        if (null == iBusinessAction) return;
        ResLogUtils.log("killRemoteService");
        try {
            android.os.Process.killProcess(iBusinessAction.getPid());
            mIsInit = false;
            instance = null;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void mainActivityDestroyed() {
        killRemoteService();

    }

    private class DataServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            iBusinessAction = IBusinessAction.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            iBusinessAction = null;
        }
    }

    public static void preStart (String h5Url) throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        if (instance == null) return;

        // 检查资源内存是否存在
        if (DataProvider.getInstance().checkCache(h5Url)) return;

        try {
            instance.iBusinessAction.preStart(h5Url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadRes (final String h5Url, String resUrl) throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        if (instance == null) return;

        try {
            instance.iBusinessAction.loadRes(h5Url, resUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static WebResourceResponse getRes (String h5Url, String resUrl) throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        return DataProvider.getInstance().getRes(h5Url, resUrl);
    }

    public static void recycleRes (String h5Url) throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        DataProvider.getInstance().recycle(h5Url);
    }

    public static void recycleRes (List<String> h5Urls) throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        DataProvider.getInstance().recycle(h5Urls);
    }

    public static void recycleAllRes () throws IllegalStateException {

        if (!mIsInit) throw new IllegalStateException("必须先进行初始化!");

        DataProvider.getInstance().recycle();
    }
}
