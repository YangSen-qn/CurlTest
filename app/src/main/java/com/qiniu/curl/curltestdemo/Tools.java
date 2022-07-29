package com.qiniu.curl.curltestdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.app.ActivityCompat;

import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

public class Tools {
    public static Context context;

    public static String getToken() {
        return "dxVQk8gyk3WswArbNhdKIwmwibJ9nFsQhMNUmtIM:F-a35Kiyll0rwhegNxj3COuET1s=:eyJzY29wZSI6Imh0dHAzLXRlc3QiLCJkZWFkbGluZSI6MzMxOTM2MzIzOH0=";
    }

    public static String getUpToken() {
        return "5cJEzNSnh3PjOHZR_E0u1HCkXw4Bw1ZSuWQI9ufz:yYbfPNo62CiCmP0z_oHdJdMmNHo=:eyJzY29wZSI6InpvbmUwLXNwYWNlIiwiZGVhZGxpbmUiOjE2NTg5MTA2MDd9";
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

    public static String getFormatDate(long timestamp) {
        Date date = new Date(timestamp);
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }


    /**
     * 获取当前apk的版本号
     *
     * @param mContext
     * @return
     */
    public static String getAppVersion(Context mContext) {
        if (mContext == null) {
            mContext = context;
        }

        String version = "";
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionCode
            version = mContext.getPackageManager().
                    getPackageInfo(mContext.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

    public static String getSystemInfo(Context mContext) {
        if (mContext == null) {
            mContext = context;
        }

        String info = android.os.Build.MODEL + "-" + android.os.Build.VERSION.RELEASE;
        return info;
    }

    private static final String NETWORK_Permissions_Reject = "permissions reject";
    private static final String NETWORK_OTHER = "other";
    private static final String NETWORK_WIFI = "wifi";
    private static final String NETWORK_2G = "2G";
    private static final String NETWORK_3G = "3G";
    private static final String NETWORK_4G = "4G";
    private static final String NETWORK_5G = "5G";
    public static String getNetworkState(Context mContext) {
        if (mContext == null) {
            mContext = context;
        }

        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE); // 获取网络服务
        if (null == connManager) { // 为空则认为无网络
            return NETWORK_OTHER;
        }

        // 获取网络类型，如果为空，返回无网络
        NetworkInfo activeNetInfo = connManager.getActiveNetworkInfo();
        if (activeNetInfo == null || !activeNetInfo.isAvailable()) {
            return NETWORK_OTHER;
        }
        // 判断是否为WIFI
        NetworkInfo wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (null != wifiInfo) {
            NetworkInfo.State state = wifiInfo.getState();
            if (null != state) {
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    return NETWORK_WIFI;
                }
            }
        }
        // 若不是WIFI，则去判断是2G、3G、4G网
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) return NETWORK_OTHER;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return NETWORK_Permissions_Reject;
        }

        int networkType = telephonyManager.getNetworkType();
        switch (networkType) {
            // 2G网络
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return NETWORK_2G;
            // 3G网络
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return NETWORK_3G;
            // 4G网络
            case TelephonyManager.NETWORK_TYPE_LTE:
            case 19:// 聚波载合 4G+
                return NETWORK_4G;
            // 5G
//            case TelephonyManager.NETWORK_TYPE_NR:// 需要 SdkVersion>=29
            case 20:// 当 SdkVersion<=28 直接写20
                return NETWORK_5G;
            default:
                return NETWORK_OTHER;
        }
    }

    public static void keepScreenLongLight(Activity activity) {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
