package com.example.myapplication.core.cache;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.example.myapplication.core.cs.ResInitService;
import com.example.myapplication.core.ResLogUtils;
import com.example.myapplication.core.cs.DataRes;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResFileUtils {

    private static final String DIST_DIR = Environment.getExternalStorageDirectory().getAbsolutePath().concat("/h5Res/");
    public static final String HEADER_EXT = ".header";


    /**
     * 判断资源是否存在，存在读到内存
     */
    public static boolean isResCacheDisk(String h5Url) {
        if (TextUtils.isEmpty(h5Url)) {
            return false;
        }
        String resDirPath = getSavePath(h5Url);
        File file = new File(resDirPath);
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return false;
            }
            readToHeap(h5Url, resDirPath);
            return true;
        }
        return false;
    }

    private static void readToHeap(String h5Url, String resDirPath) {
        Intent intent = new Intent(DataRes.MSG_TYPE_PERFORM_READ_DATA);
        intent.putExtra("h5Url", h5Url);
        intent.putExtra("resDirPath",resDirPath);
        ResInitService.mContext.sendBroadcast(intent);
    }

    public static boolean writeFile(String str, String filePath) {
        return writeFile(str.getBytes(), filePath);
    }

    /**
     * Write bytes to the specific file.
     */
    public static boolean writeFile(byte[] content, String filePath) {
        File file = new File(filePath);
        FileOutputStream fos = null;
        try {
            if (!file.exists() && !file.createNewFile()) {
                return false;
            }
            fos = new FileOutputStream(file);
            fos.write(content);
            fos.flush();
            return true;
        } catch (Throwable e) {
            ResLogUtils.log("writeFile error:(" + filePath + ") " + e.getMessage());
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (Throwable e) {
                    ResLogUtils.log("writeFile close error:(" + filePath + ") " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * 资源响应头字符串化
     */
    public static String convertHeadersToString(Map<String, List<String>> headers) {
        if (headers != null && headers.size() > 0) {
            StringBuilder headerString = new StringBuilder();
            Set<Map.Entry<String, List<String>>> entries =  headers.entrySet();
            for (Map.Entry<String, List<String>> entry : entries) {
                String key = entry.getKey();
                if (!TextUtils.isEmpty(key)) {
                    List<String> values = entry.getValue();
                    for (String value : values) {
                        if (!TextUtils.isEmpty(value)) {
                            headerString.append(key).append(" : ");
                            headerString.append(value).append("\r\n");
                        }
                    }
                }
            }
            return headerString.toString();
        }

        return "";
    }

    /**
     * 获取资源相应头
     */
    public static Map<String, List<String>> getHeaderFromLocalCache (String headerPath) {
        Map<String, List<String>> headers = new HashMap<>();
        File headerFile = new File(headerPath);
        if (headerFile.exists()) {
            String headerString = readFile(headerFile);
            if (!TextUtils.isEmpty(headerString)) {
                String[] headerArray = headerString.split("\r\n");
                if (headerArray.length > 0) {
                    List<String> tmpHeaderList;
                    for (String header : headerArray) {
                        String[] keyValues = header.split(" : ");
                        if (keyValues.length == 2) {
                            String key = keyValues[0].trim();
                            tmpHeaderList = headers.get(key.toLowerCase());
                            if (null == tmpHeaderList) {
                                tmpHeaderList = new ArrayList<String>(1);
                                headers.put(key.toLowerCase(), tmpHeaderList);
                            }
                            tmpHeaderList.add(keyValues[1].trim());
                        }
                    }
                }
            }
        }

        return headers;
    }

    /**
     * 获取资源存放目录位置
     */
    private static String getSavePath (String h5Url) {
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
            file.mkdirs();
        }
        return dirPath;
    }

    /**
     * 获取资源文件名
     */
    public static String getResDiskName (String url) {
        if (TextUtils.isEmpty(url)) {
            return System.currentTimeMillis() + "";
        }
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }

    /**
     * 获取资源响应头保存路径
     */
    public static String getResResponseHeaderPath(String mH5Url, String resUrl) {
        return getSavePath(mH5Url) + getResResponseHeader(resUrl);
    }

    /**
     * 获取资源响应名
     */
    public static String getResResponseHeader (String url) {
        if (TextUtils.isEmpty(url)) {
            return System.currentTimeMillis() + "";
        }
        Uri uri = Uri.parse(url);
        String lastPathSegment = uri.getLastPathSegment();
        if (TextUtils.isEmpty(lastPathSegment) || !lastPathSegment.contains(".")) {
            return System.currentTimeMillis() + "";
        }
        return lastPathSegment.substring(0, lastPathSegment.indexOf(".")) + HEADER_EXT;
    }

    /**
     * 保存到磁盘
     */
    public static void writeToDisk (String resUrl, InputStream is, String h5Url) {
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            ResLogUtils.log("sd卡不可用 ！");
            return;
        }
        if (TextUtils.isEmpty(resUrl) || is == null || TextUtils.isEmpty(h5Url)) {
            return;
        }
        FileOutputStream fos = null;
        try {
            byte[] buf = new byte[2048];
            int len = 0;
            String savePath = getSavePath(h5Url);
            File file = new File(savePath, getResDiskName(resUrl));
            fos = new FileOutputStream(file);
            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
            fos.flush();
            ResLogUtils.log("download " + resUrl + " success");

            readToHeap(h5Url, savePath);
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

    /**
     * 读资源文件
     */
    public static byte[] readFileToBytes(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        // read
        BufferedInputStream bis = null;
        ByteArrayOutputStream out = null;
        byte[] rtn = null;
        int n;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int size = (int) file.length();
            if (size > 1024 * 12) {
                out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024 * 4];
                while ((n = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, n);
                }
                rtn = out.toByteArray();
            } else {
                rtn = new byte[size];
                n = bis.read(rtn);
            }
        } catch (Throwable e) {
            ResLogUtils.log("readFile error:(" + file.getName() + ") " + e.getMessage());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception e) {
                    ResLogUtils.log("readFile close error:(" + file.getName() + ") " + e.getMessage());
                }
            }
        }
        return rtn;
    }

    static String readFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            return null;
        }

        // read
        BufferedInputStream bis = null;
        InputStreamReader reader = null;
        char[] buffer;
        String rtn = null;
        int n;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            reader = new InputStreamReader(bis);
            int size = (int) file.length();
            if (size > 1024 * 12) {
                buffer = new char[1024 * 4];
                StringBuilder result = new StringBuilder(1024 * 12);
                while (-1 != (n = reader.read(buffer))) {
                    result.append(buffer, 0, n);
                }
                rtn = result.toString();
            } else {
                buffer = new char[size];
                n = reader.read(buffer);
                rtn = new String(buffer, 0, n);
            }
        } catch (Throwable e) {
            ResLogUtils.log("readFile error:(" + file.getName() + ") " + e.getMessage());
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Exception e) {
                    ResLogUtils.log("readFile close error:(" + file.getName() + ") " + e.getMessage());
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    ResLogUtils.log("readFile close error:(" + file.getName() + ") " + e.getMessage());
                }
            }
        }
        return rtn;
    }

    /**
     * 删除本地资源文件
     */
    public static boolean deleteRes(String h5Url, String resUrl) {
        if (TextUtils.isEmpty(h5Url) || TextUtils.isEmpty(resUrl)) {
            return false;
        }
        boolean deleteSuccess = true;
        File file = new File(getSavePath(h5Url) + getResDiskName(resUrl));
        if (file.exists() && file.isFile()) {
            deleteSuccess = file.delete();
        }
        return deleteSuccess;
    }

    /**
     * 删除所有资源文件
     */
    public static boolean deleteAllResFiles(File file) {
        boolean deleteSuccess = true;
        if (null != file && file.exists()) {
            if (file.isFile()) {
                deleteSuccess = file.delete();
            } else if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                if (null != childFiles) {
                    for (File childFile : childFiles) {
                        deleteSuccess &= deleteAllResFiles(childFile);
                    }
                }
            }
        }
        return deleteSuccess;
    }
}
