package com.example.myapplication.core;

import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.tencent.smtt.export.external.interfaces.WebResourceResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Response;
import okhttp3.ResponseBody;

class ResFileUtils {

    private static String DIST_DIR = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/k12/h5Res/");


    /**
     * 判断资源是否存在，存在读到内存
     */
    static boolean isResCacheDisk(UnLineResManager.ResCache resCache) {
        if (resCache == null || TextUtils.isEmpty(resCache.getH5Url())) {
            return false;
        }
        File file = new File(getResDiskName(resCache.getH5Url()));
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            readToHeap(files, resCache);
        }
        return false;
    }

    private static void readToHeap(File[] files, UnLineResManager.ResCache resCache) {
        if (files.length == 0) {
            return;
        }
        for (File resFile : files) {
            if (resFile == null || !resFile.exists() || !resFile.isFile()) {
                continue;
            }
            try {
                String fileName = resFile.getName();
                WebResourceResponse response = WebResponseTranslator.transform(fileName, new FileInputStream(resFile));
                resCache.addRes(fileName, response);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 删除本地资源文件
     */
    static void deleteRes(String h5Url, String resUrl) {
        if (TextUtils.isEmpty(h5Url) || TextUtils.isEmpty(resUrl)) {
            return;
        }
        File file = new File(getSavePath(h5Url) + getResDiskName(resUrl));
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取资源存放目录位置
     */
    private static String getSavePath(String h5Url) {
        String h5Name = getResDiskName(h5Url);
        String dirPath = DIST_DIR;
        if (!TextUtils.isEmpty(h5Name)) {
            int index = h5Name.indexOf(".html");
            if (index > 0) {
                String name = h5Name.substring(0, index);
                dirPath += name + "/";
            }
        }
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdir();
        }
        return dirPath;
    }

    /**
     * 获取资源文件名
     */
    static String getResDiskName(String url) {
        if (TextUtils.isEmpty(url)) {
            return System.currentTimeMillis() + "";
        }
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }

    /**
     * 保存到磁盘
     */
    static void writeToDisk(long startTime, String resUrl, Response response, UnLineResManager.ResCache resCache) {
        if (TextUtils.isEmpty(resUrl) || response == null || resCache == null) {
            return;
        }
        ResponseBody body = response.body();
        if (body == null) {
            return;
        }
        InputStream is = body.byteStream();
        FileOutputStream fos = null;
        try {
            byte[] buf = new byte[2048];
            int len = 0;
            String savePath = getSavePath(resCache.getH5Url());
            long total = body.contentLength();
            File file = new File(savePath, getResDiskName(resUrl));
            fos = new FileOutputStream(file);
            long sum = 0;
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
                sum += len;
                int progress = (int) (sum * 1.0f / total * 100);
                ResLogUtils.log("download " + resUrl + " progress : " + progress);
            }
            fos.flush();
            ResLogUtils.log("download " + resUrl + " success !! totalTime = "+ (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            e.printStackTrace();
            ResLogUtils.log("download " + resUrl + " failed : " + e.getMessage());
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
            }
        }
    }
}
