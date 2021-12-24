package com.qiniu.curl.curltestdemo;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.http.dns.Dns;
import com.qiniu.android.http.dns.IDnsNetworkAddress;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.GlobalConfiguration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.LogUtil;
import com.qiniu.client.curl.CurlClient;

import org.json.JSONObject;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Uploader implements Dns {

    private static final String UploadHost = "upload.qiniup.com";
    private static final String[] UploadIpList = new String[]{
            "111.1.36.180", "218.98.28.87", "218.98.28.28",
            "112.13.172.42", "112.13.172.39", "223.111.225.57",
            "117.148.177.151", "223.111.225.66", "223.111.225.67",
            "124.160.115.130"};
    private static final List<IDnsNetworkAddress> UploadAddress = getUploadAddress();
    private static List<IDnsNetworkAddress> getUploadAddress(){
        List<IDnsNetworkAddress> addresses = new ArrayList<>();
        for (String ip : UploadIpList) {
            addresses.add(new DnsAddress(UploadHost, ip));
        }
        return addresses;
    }

    private static final Uploader Instance = new Uploader();

    public static Uploader getInstance() {
        return Instance;
    }
    private Uploader() {
        LogUtil.enableLog(true);
        GlobalConfiguration.getInstance().dns = this;
        GlobalConfiguration.getInstance().enableHttp3 = true;
    }

    public void uploadFile(String file, String key, Complete complete) {
        uploadFile(file, key, false, null, complete);
    }

    public void uploadFile(String file, String key, boolean useHttp3, UpCancellationSignal cancellationSignal, Complete complete) {
        Configuration.Builder builder = new Configuration.Builder()
                .putThreshold(1024 * 1024 * 4)
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .chunkSize(1024 * 1024)
                .useConcurrentResumeUpload(true)
                .concurrentTaskCount(3)
                .zone(new FixedZone(new String[]{UploadHost}));

        if (useHttp3) {
            builder.requestClient(new CurlClient());
        }

        Configuration cfg = builder.build();
        UploadOptions options = new UploadOptions(null, null, false, null, cancellationSignal);
        UploadManager manager = new UploadManager(cfg);
        manager.put(file, key, Tools.getToken(), new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null && (info.isOK() || info.statusCode == 614)) {
                    complete.complete(true, null);
                } else {
                    complete.complete(false, info != null ? info.error : null);
                }
            }
        }, options);
    }

    public void uploadData(String log, String key, Complete complete) {
        Configuration cfg = new Configuration.Builder()
                .putThreshold(1024 * 1024 * 4)
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .chunkSize(1024 * 1024)
                .useConcurrentResumeUpload(true)
                .concurrentTaskCount(3)
                .zone(FixedZone.zone0)
                .build();
        UploadManager manager = new UploadManager(cfg);
        manager.put(log.getBytes(StandardCharsets.UTF_8), key, Tools.getToken(), new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null && (info.isOK() || info.statusCode == 614)) {
                    complete.complete(true, null);
                } else {
                    complete.complete(false, info != null ? info.error : null);
                }
            }
        }, null);
    }



    public interface Complete {
        void complete(boolean isSuccess, String error);
    }


    @Override
    public List<IDnsNetworkAddress> lookup(String hostname) throws UnknownHostException {
        if (!hostname.equals(UploadHost)) {
            return null;
        }
        return UploadAddress;
    }

    private static class DnsAddress implements IDnsNetworkAddress {
        private final String host;
        private final String ip;

        private DnsAddress(String host, String ip) {
            this.host = host;
            this.ip = ip;
        }

        @Override
        public String getHostValue() {
            return host;
        }

        @Override
        public String getIpValue() {
            return ip;
        }

        @Override
        public Long getTtlValue() {
            return 120L;
        }

        @Override
        public String getSourceValue() {
            return "customized";
        }

        @Override
        public Long getTimestampValue() {
            return new Date().getTime() / 1000;
        }
    }
}
