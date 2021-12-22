package com.qiniu.curl.curltestdemo;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

public class Uploader {

    public static void uploadFile(String file, String key, boolean useHttp3, UpCancellationSignal cancellationSignal, Complete complete) {
        Configuration cfg = new Configuration.Builder()
                .putThreshold(1024 * 1024 * 4)
                .resumeUploadVersion(Configuration.RESUME_UPLOAD_VERSION_V2)
                .chunkSize(1024 * 1024)
                .useConcurrentResumeUpload(true)
                .concurrentTaskCount(3)
                .zone(FixedZone.zone0)
                .build();
        UploadOptions options = new UploadOptions(null, null, false, null, cancellationSignal);
        UploadManager manager = new UploadManager(cfg);
        manager.put(file, key, Tools.getToken(), new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null && info.isOK()) {
                    complete.complete(true, null);
                } else {
                    complete.complete(false, info != null ? info.error : null);
                }
            }
        }, options);
    }

    public static void uploadLog(String log, String key, Complete complete) {
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
                if (info != null && info.isOK()) {
                    complete.complete(true, null);
                } else {
                    complete.complete(false, info != null ? info.error : null);
                }
            }
        }, null);
    }

    private interface Complete {
        void complete(boolean isSuccess, String error);
    }
}
