package com.qiniu.curl.curltestdemo;

import com.qiniu.android.storage.UpCancellationSignal;
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

    private String network;
    private int type;
    private boolean isResumeV2;
    private long fileSize;
    private String key;
    private UpCancellationSignal cancellationSignal;

    private long duration = 0;
    private boolean isSuccess = false;
    private String error = null;

    private Logger logger;
    private int status = StatusWaiting;

    public UploadTask(int type, long fileSize, String key, boolean isResumeV2, Logger logger, UpCancellationSignal cancellationSignal) {
        this.type = type;
        this.fileSize = fileSize;
        this.key = key;
        this.isResumeV2 = isResumeV2;
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
        network = Tools.getNetworkState(null);

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
        logger.log(true, description());
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

    public long getDuration() {
        return duration;
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
            logger.log(false,"error: create log file failed");
            return false;
        }

        Uploader.getInstance().uploadFile(filepath, key, type == TypeHttp3, isResumeV2, cancellationSignal, new Uploader.Complete() {
            @Override
            public void complete(boolean isSuccess, String error) {
                complete.complete(isSuccess, error);
            }
        });
        return true;
    }

    private String description() {
        String desc = "";
        desc += key;
        desc += " Network:" + network;
        desc += " ResumeV2:" + isResumeV2;
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
            jsonObject.putOpt("network", network);
            jsonObject.putOpt("is_resume_v2", isResumeV2);
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
            task.network = jsonObject.getString("network");
            task.isResumeV2 = jsonObject.getBoolean("is_resume_v2");
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
