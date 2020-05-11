package com.example.myapplication.core;
import android.util.Log;

public class ResLogUtils {
    private static final String TAG = "unLine res (pid : %d) => ";

    public static void log(String content) {
        Log.d(String.format(TAG, android.os.Process.myPid()), content);
    }
}
