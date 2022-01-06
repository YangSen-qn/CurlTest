package com.qiniu.curl.curltestdemo;

import com.pgyersdk.crash.PgyCrashManager;
import com.qiniu.android.storage.UpCancellationSignal;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UploadJob {
    private static final String JobCacheName = "UploadJob.json";

    private Logger logger;
    private BufferedWriter debugWriter;
    private String jobName;
    private UpCancellationSignal cancellationSignal;

    private long createTimestamp;
    private boolean isInfoUploaded;
    private boolean isDebugFileUploaded;
    private String debugFile;
    private boolean isCompleted = false;
    private UploadTaskGroup currentTaskGroup;
    private List<UploadTaskGroup> taskGroups = new ArrayList<>();

    public UploadJob(String jobName, Logger loggerParam, UpCancellationSignal cancellationSignal) {
        this.createTimestamp = new Date().getTime();
        this.jobName = jobName;
        this.debugFile = Tools.tmpDir() + "/" + jobName + "_debug.log";
        this.logger = new Logger() {
            @Override
            public void log(boolean isDetail, String info) {
                if (info != null && info.length() > 0) {
                    info += "\n";
                    appendDebugInfo(info);
                    loggerParam.log(isDetail, info);
                }
            }
        };
        this.cancellationSignal = cancellationSignal;
        createTasks();
    }

    private UploadJob() {
    }

    private void createTasks() {
        String error = loadTasksFromLocal();
        if (error == null) {
            logger.log(false, "从本地成功加载 Job:" + jobName);
            return;
        }
        logger.log(false, "从本地加载 Job:" + jobName + " 失败:" + error);
        PgyCrashManager.reportCaughtException(Tools.context, new Exception(error));

        for (TestCase test : TestCase.testCases) {
            taskGroups.add(new UploadTaskGroup(test, jobName, logger, cancellationSignal));
        }
    }

    public void prepare() {
        createDebugWriter();
        setCompleted(false);
        for (UploadTaskGroup group : taskGroups) {
            group.prepare();
        }
    }

    public void run() {
        prepare();

        logger.log(false,"\n");
        logger.log(false, "App  Version:" + Tools.getAppVersion(null));
        logger.log(false, "System  Info:" + Tools.getSystemInfo(null));
        logger.log(false, "Network Info:" + Tools.getNetworkState(null));
        logger.log(false,"\n");
        logger.log(false,"job:" + jobName + " start");
        logger.log(false,"\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (UploadTaskGroup group : taskGroups) {
                        if (cancellationSignal != null && cancellationSignal.isCancelled()) {
                            logger.log(false,"job:" + jobName + " cancelled");
                            break;
                        }
                        currentTaskGroup = group;
                        group.run();
                        saveTasksToLocal();
                    }
                    releaseDebugWriter();
                    logger.log(false,"job:" + jobName + " end");
                    logger.log(false,"\n");
                    setCompleted(true);
                } catch (Exception e) {
                    logger.log(false,"[Error]:" + e.toString());
                    PgyCrashManager.reportCaughtException(Tools.context, e);
                    setCompleted(true);
                }
            }
        }).start();
    }

    public UploadTaskGroup currentTask() {
        return currentTaskGroup;
    }

    public int taskCount() {
        return taskGroups.size();
    }

    public int executedTaskCount() {
        int count = 0;
        for (UploadTaskGroup task : taskGroups) {
            if (task.isCompleted()) {
                count += 1;
            }
        }
        return count;
    }

    public String getJobName() {
        return jobName;
    }

    public String getDebugFilePath() {
        return debugFile;
    }

    public synchronized boolean isCompleted() {
        return isCompleted;
    }

    private synchronized void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setInfoUploaded(boolean infoUploaded) {
        isInfoUploaded = infoUploaded;
        saveTasksToLocal();
    }

    public void setDebugFileUploaded(boolean debugFileUploaded) {
        isDebugFileUploaded = debugFileUploaded;
        saveTasksToLocal();
    }

    public long getCreateTimestamp() {
        return createTimestamp;
    }

    public String reportInfo() {
        String info = "";
        for (UploadTaskGroup group : taskGroups) {
            if (group != null) {
                String groupInfo = group.reportInfo();
                if (groupInfo != null && groupInfo.length() > 0) {
                    info += groupInfo + "\n";
                }
            }
        }
        return info;
    }

    // -------- 数据缓存 -----------
    private String getCacheName() {
        return jobName + "_" + JobCacheName;
    }
    private String loadTasksFromLocal() {
        String cacheName = getCacheName();
        byte[] data = Cache.getCacheData(cacheName);
        if (data == null) {
            return "not find cache data for key:" + cacheName;
        }

        UploadJob job = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(new String(data));
            job = jobFromJson(jsonObject);
        } catch (Exception e) {
            return e.toString();
        }

        if (job == null) {
            return "parse cache data error";
        }
        this.createTimestamp = job.createTimestamp;
        this.taskGroups = job.taskGroups;
        return null;
    }

    private String saveTasksToLocal() {
        JSONObject jsonObject = toJsonData();
        if (jsonObject == null) {
            return "job to json error";
        }

        byte[] jsonData = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
        if (jsonData == null) {
            return "job to json data error";
        }

        return Cache.cacheData(getCacheName(), jsonData);
    }

    public void clearJobCacheIfNeeded() {
        removeDebugInfoFromLocalIfNeeded();
        removeTasksInfoFromLocalIfNeeded();
    }

    private void removeTasksInfoFromLocalIfNeeded() {
        if (executedTaskCount() != taskCount()) {
            return;
        }
        Cache.removeCache(getCacheName());
    }

    private void createDebugWriter() {
        if (debugWriter != null) {
            return;
        }
        try {
            this.debugWriter = new BufferedWriter(new FileWriter(this.debugFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void releaseDebugWriter() {
        if (debugWriter != null) {
            try {
                debugWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            debugWriter = null;
        }
    }

    private void appendDebugInfo(String info) {
        if (debugWriter == null) {
            return;
        }
        try {
            debugWriter.write(info);
            debugWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeDebugInfoFromLocalIfNeeded() {
        if (executedTaskCount() != taskCount()) {
            return;
        }

        File file = new File(this.debugFile);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }



    //----------- json ------------
    private JSONObject toJsonData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("job_name", jobName);
            jsonObject.putOpt("is_info_uploaded", isInfoUploaded);
            jsonObject.putOpt("is_debug_file_uploaded", isDebugFileUploaded);
            jsonObject.putOpt("create_timestamp", createTimestamp);
            JSONArray taskJsonArray = new JSONArray();
            for (UploadTaskGroup taskGroup : taskGroups) {
                JSONObject taskJson = taskGroup.toJsonData();
                if (taskJson == null) {
                    return null;
                }
                taskJsonArray.put(taskJson);
            }
            jsonObject.putOpt("task_groups", taskJsonArray);
        } catch (JSONException e) {
            jsonObject = null;
        }
        return jsonObject;
    }

    private UploadJob jobFromJson(JSONObject jsonObject) {
        UploadJob job = new UploadJob();
        try {
            job.jobName = jsonObject.getString("job_name");
            job.isInfoUploaded = jsonObject.getBoolean("is_info_uploaded");
            job.isDebugFileUploaded = jsonObject.getBoolean("is_debug_file_uploaded");
            job.createTimestamp = jsonObject.getLong("create_timestamp");
            job.taskGroups = new ArrayList<>();
            JSONArray taskJsonArray = jsonObject.getJSONArray("task_groups");
            for (int i = 0; i < taskJsonArray.length(); i++) {
                UploadTaskGroup taskGroup = UploadTaskGroup.taskGroupFromJson(taskJsonArray.getJSONObject(i), logger, cancellationSignal);
                if (taskGroup == null) {
                    return null;
                }
                job.taskGroups.add(taskGroup);
            }
        } catch (JSONException e) {
            logger.log(false, e.getMessage());
            return null;
        }
        return job;
    }
}
