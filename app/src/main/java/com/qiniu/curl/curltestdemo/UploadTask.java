package com.qiniu.curl.curltestdemo;

import com.qiniu.android.common.FixedZone;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.qiniu.android.storage.UploadOptions;
import com.qiniu.android.utils.Utils;
import com.qiniu.android.utils.Wait;

import org.json.JSONException;
import org.json.JSONObject;

public class UploadTask implements Runnable {
    public static final int TypeHttp2 = 1;
    public static final int TypeHttp3 = 2;

    public static final int StatusWaiting = 10;
    public static final int StatusUploading = 11;
    public static final int StatusCancelled = 12;
    public static final int StatusCompleted = 13;

    private int type;
    private long fileSize;
    private String key;
    private UpCancellationSignal cancellationSignal;

    private long duration = 0;
    private boolean isSuccess = false;
    private String error = null;

    private Logger logger;
    private int status = StatusWaiting;

    public UploadTask(int type, long fileSize, String key, Logger logger, UpCancellationSignal cancellationSignal) {
        this.type = type;
        this.fileSize = fileSize;
        this.key = key;
        this.logger = logger;
        this.cancellationSignal = cancellationSignal;
    }

    private UploadTask() {}

    public void prepare() {
        if (status == StatusCancelled || status == StatusUploading) {
            status = StatusWaiting;
            duration = 0;
        }
    }

    public void run() {
        if (status == StatusCancelled || status == StatusCompleted) {
            return;
        }

        if (cancellationSignal != null && cancellationSignal.isCancelled()) {
            setStatus(StatusCancelled);
            return;
        }
        setStatus(StatusUploading);

        final Wait wait = new Wait();
        long start = Utils.currentTimestamp();
        boolean upload = upload(new Complete() {
            @Override
            public void complete(boolean success, String err) {
                error = err;
                isSuccess = success;
                wait.stopWait();
            }
        });

        if (upload) {
            wait.startWait();

            if (cancellationSignal != null && cancellationSignal.isCancelled()) {
                setStatus(StatusCancelled);
            } else {
                setStatus(StatusCompleted);
            }
        }

        long end = Utils.currentTimestamp();
        duration = end - start;
        logger.log(description());
    }

    public boolean isComplete() {
        return status() == StatusCompleted;
    }

    public boolean isSuccess() {
        return status() == StatusCompleted && isSuccess;
    }

    public boolean needUpload() {
        return status() == StatusWaiting;
    }

    public synchronized int status() {
        return status;
    }

    public synchronized void setStatus(int status) {
        this.status = status;
    }

    private boolean upload(Complete complete) {
        String filepath = Tools.getFileOfSize(fileSize);
        if (filepath == null || filepath.length() == 0) {
            logger.log("error: create log file failed");
            return false;
        }

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
        manager.put(filepath, key, Tools.getToken(), new UpCompletionHandler() {
            @Override
            public void complete(String key, ResponseInfo info, JSONObject response) {
                if (info != null && info.isOK()) {
                    complete.complete(true, null);
                } else {
                    complete.complete(false, info != null ? info.error : null);
                }
            }
        }, options);
        return true;
    }

    private String description() {
        String desc = "";
        desc += key;
        desc += " " + (type == UploadTask.TypeHttp2 ? "http2" : "http3");
        desc += " Duration:" + duration + "ms";
        desc += " Success:" + (isSuccess ? "true" : " false");
        desc += " Error:" + error;
        return desc;
    }

    //----------- json ------------
    public JSONObject toJsonData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("type", type);
            jsonObject.putOpt("file_size", fileSize);
            jsonObject.putOpt("key", key);
            jsonObject.putOpt("duration", duration);
            jsonObject.putOpt("is_success", isSuccess);
            jsonObject.putOpt("error", error);
            jsonObject.putOpt("status", status);
        } catch (JSONException e) {
            jsonObject = null;
        }
        return jsonObject;
    }

    public static UploadTask taskFromJson(JSONObject jsonObject, Logger logger, UpCancellationSignal cancellationSignal) {
        if (jsonObject == null) {
            return null;
        }

        UploadTask task = new UploadTask();
        task.logger = logger;
        task.cancellationSignal = cancellationSignal;
        try {
            task.type = jsonObject.getInt("type");
            task.fileSize = jsonObject.getLong("file_size");
            task.key = jsonObject.getString("key");
            task.duration = jsonObject.getLong("duration");
            task.isSuccess = jsonObject.getBoolean("is_success");
            task.error = jsonObject.optString("error");
            task.status = jsonObject.getInt("status");
        } catch (Exception ignored) {
            return null;
        }
        return task;
    }

    private interface Complete {
        void complete(boolean isSuccess, String error);
    }
}
