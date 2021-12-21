package com.qiniu.curl.curltestdemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Cache {
    private static final String cacheDir = Tools.tmpDir();

    public static String removeCache(String key) {
        if (cacheDir.length() == 0 || key == null) {
            return "remove cache error: cacheDir or key empty";
        }

        File f = new File(cacheDir, key);
        if (!f.exists()) {
            return "remove cache error: cache file not exist";
        }

        if (!f.delete()) {
            return "remove cache error: cache delete failed";
        }

        return null;
    }

    public static byte[] getCacheData(String key) {
        if (cacheDir.length() == 0 || key == null) {
            return null;
        }

        File f = new File(cacheDir,key);
        FileInputStream fi = null;
        byte[] data = null;
        int read = 0;
        try {
            data = new byte[(int) f.length()];
            fi = new FileInputStream(f);
            read = fi.read(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fi != null) {
            try {
                fi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (read == 0) {
            return null;
        }

        return data;
    }

    public static String cacheData(String key, byte[] data) {
        if (cacheDir.length() == 0) {
            return "cacheDir can't empty";
        }

        if (key == null) {
            return "key can't empty";
        }

        String error = null;
        File f = new File(cacheDir, key);
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(f);
            fo.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        if (fo != null) {
            try {
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return error;
    }
}
