package com.example.myapplication.core.download;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadTask {

    public static final int STATE_INITIATE = 0;

    public static final int STATE_QUEUEING = 1;

    public static final int STATE_DOWNLOADING = 2;

    public static final int STATE_DOWNLOADED = 3;

    public static final int STATE_LOAD_FROM_CACHE = 4;

    public String mResourceUrl;

    public String mIpAddress;

    public String mCookie;

    public Map<String, List<String>> mRspHeaders;

    public InputStream mInputStream;

    public AtomicInteger mState = new AtomicInteger(STATE_INITIATE);

    public final AtomicBoolean mWasInterceptInvoked = new AtomicBoolean(false);

    public List<DownloadCallback> mCallbacks = new ArrayList<DownloadCallback>();

}
