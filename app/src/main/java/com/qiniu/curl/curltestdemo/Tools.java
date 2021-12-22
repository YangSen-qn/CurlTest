package com.qiniu.curl.curltestdemo;

import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.FileHandler;

public class Tools {

    public static String getToken() {
        return "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:Lg3QQz1QnODk-PxSfnE8xpR27lM=:eyJzY29wZSI6Imh0dHAzLXRlc3QiLCJkZWFkbGluZSI6MTY1MDUyMjg5NH0=";
    }

    // 单位：kb
    public synchronized static String getFileOfSize(long kiloSize) {
        String fileName = "qiniu-demo-file-" + kiloSize + "KB.tmp";
        String tmpDir = tmpDir();
        if (tmpDir.length() == 0) {
            return null;
        }

        String filepath = tmpDir + "/" + fileName;
        File file = new File(filepath);
        if (file.exists() && file.isFile()) {
            return filepath;
        }

        FileOutputStream fos = null;
        try {
            long size = (long) (1024 * kiloSize);
            boolean isSuccess = file.createNewFile();
            if (!isSuccess) {
                return null;
            }

            fos = new FileOutputStream(file);
            byte[] b = getByte(1023 * 4, 0);
            long s = 0;
            while (s < size) {
                int l = (int) Math.min(b.length, size - s);
                fos.write(b, 0, l);
                s += l;
            }
            fos.flush();
        } catch (Exception e) {
            filepath = "";
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return filepath;
    }

    private static byte[] getByte(int len, int index) {
        byte[] b = new byte[len];
        b[0] = (byte) (index & 0xFF);
        for (int i = 1; i < len; i++) {
            b[i] = 'b';
        }
        b[len - 2] = '\r';
        b[len - 1] = '\n';
        return b;
    }

    public static String tmpDir() {
        String path = Utils.sdkDirectory();
        path = path + "/" + "/tmp";
        File file = new File(path);
        if ((!file.exists() || file.isFile()) && !file.mkdirs()) {
            return "";
        } else {
            return path;
        }
    }

    public static String getMemoryDesc(long kiloSize) {
        DecimalFormat df = new DecimalFormat("#.00");
        if (kiloSize < 1024) {
            return kiloSize + "KB";
        } else if (kiloSize < 1024 * 1024) {
            return df.format((double) kiloSize / 1024) + "M";
        } else {
            return df.format((double) kiloSize / 1024 / 1024) + "G";
        }
    }
}
