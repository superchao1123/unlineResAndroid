package com.example.myapplication.core.download;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadManager implements Handler.Callback  {

    /**
     * 单例
     */
    private static volatile DownloadManager instance;
    private final OkHttpClient mOkHttpClient;
    public static DownloadManager getInstance() {
        if (instance == null) {
            synchronized (DownloadManager.class) {
                if (instance == null) {
                    instance = new DownloadManager();
                }
            }
        }
        return instance;
    }

    private DownloadManager() {
        mOkHttpClient = new OkHttpClient();
        mQueue = new DownloadQueue();
        HandlerThread queueThread = new HandlerThread("Res_Download_Thread");
        queueThread.start();
        mHandler = new Handler(queueThread.getLooper(), this);
        mLoadingCount = new AtomicInteger(0);
        executorServiceImpl = new ThreadPoolExecutor(1, 6,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                new SessionThreadFactory());
    }

    /**
     * 请求队列
     */
    private static final int MAX_REQUEST_COUNT = 3;
    private static final int MSG_TYPE_POP = 0;
    private static final int MSG_TYPE_PUSH = 1;
    private final DownloadQueue mQueue;
    private Handler mHandler;
    private AtomicInteger mLoadingCount;

    private static class DownloadQueue extends LinkedHashMap<String, DownloadTask> {

        synchronized DownloadTask pop () {
            if (values().iterator().hasNext()) {
                DownloadTask task = values().iterator().next();
                return remove(task.mResourceUrl);
            }
            return null;
        }

        synchronized void push (DownloadTask task) {
            if (task != null && !TextUtils.isEmpty(task.mResourceUrl)) {
                put(task.mResourceUrl, task);
            }
        }
    }

    @Override
    public boolean handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_TYPE_PUSH: {
                DownloadTask task = (DownloadTask) msg.obj;
                mQueue.push(task);
                task.mState.set(DownloadTask.STATE_QUEUEING);
                break;
            }
            case MSG_TYPE_POP: {
                if (!mQueue.isEmpty()) {
                    DownloadTask task = mQueue.pop();
                    startDownload(task);
                }
                break;
            }
            default:
                break;
        }
        return false;
    }

    /**
     * 线程池
     */
    private final ExecutorService executorServiceImpl;

    private static class SessionThreadFactory implements ThreadFactory {

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final static String NAME_PREFIX = "pool_res_thread_";

        SessionThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            this.group = securityManager != null ? securityManager.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        public Thread newThread(@NonNull Runnable r) {
            Thread thread = new Thread(this.group, r, NAME_PREFIX + this.threadNumber.getAndIncrement(), 0L);
            if (thread.isDaemon()) {
                thread.setDaemon(false);
            }

            if (thread.getPriority() != 5) {
                thread.setPriority(5);
            }

            return thread;
        }
    }

    /**
     * 下载资源
     * @param resUrl        资源URL
     * @param ipAddress     资源域名ip
     */
    public void load (String resUrl, String ipAddress, String cookie, DownloadCallback callback) {
        if (TextUtils.isEmpty(resUrl)) {
            return;
        }

        final DownloadTask task = new DownloadTask();
        task.mResourceUrl = resUrl;
        task.mCallbacks.add(callback);
        task.mCallbacks.add(new DownloadCallback.SimpleDownloadCallback() {
            @Override
            public void onFinish() {
                task.mState.set(DownloadTask.STATE_DOWNLOADED);
                mHandler.sendEmptyMessage(MSG_TYPE_POP);
            }
        });

        task.mIpAddress = ipAddress;
        task.mCookie = cookie;
        if (mLoadingCount.get() < MAX_REQUEST_COUNT) {
            startDownload(task);
        } else {
            Message enqueueMsg = mHandler.obtainMessage(MSG_TYPE_PUSH, task);
            mHandler.sendMessage(enqueueMsg);
        }
    }

    /**
     * 下载相同ip，cookie资源包
     * @param resLinks     资源url
     * @param ipAddress    资源域名ip
     */
    public void load (List<String> resLinks, String ipAddress, String cookie, DownloadCallback callback) {
        if (resLinks == null || resLinks.isEmpty()) {
            return;
        }
        for (final String link : resLinks) {
            if (TextUtils.isEmpty(link)) {
                continue;
            }
            load(link, ipAddress, cookie, callback);
        }
    }

    private void startDownload(final DownloadTask task) {
        executorServiceImpl.execute(new Runnable() {
            @Override
            public void run() {
                mLoadingCount.incrementAndGet();
                task.mState.set(DownloadTask.STATE_DOWNLOADING);
                request(task);
            }
        });
    }

    private void request(final DownloadTask task) {
        if (task == null || TextUtils.isEmpty(task.mResourceUrl)) {
            return;
        }
        String resUrl = task.mResourceUrl;

        // dns优化
        try {
            if (!TextUtils.isEmpty(task.mIpAddress)) {
                URL url = new URL(resUrl);
                String originHost = url.getHost();
                url = new URL(resUrl.replace(originHost, task.mIpAddress));
                resUrl = url.getPath();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        final Request request = new Request.Builder().url(resUrl).header("Cookie", task.mCookie).build();
        final String finalResUrl = resUrl;
        mOkHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response == null || response.code() != 200) {
                    return;
                }
                try {
                    byte[] bytes = response.body().bytes();
                    for (DownloadCallback callback : task.mCallbacks) {
                        if (callback == null) {
                            continue;
                        }
                        Headers headers = response.headers();
                        callback.onSuccess(finalResUrl, bytes, headers.toMultimap());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
