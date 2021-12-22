package com.qiniu.curl.curltestdemo;

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
import java.util.List;

public class UploadJob {
    private static final String JobCacheName = "UploadJob.json";

    private Logger logger;
    private String debugFile;
    private BufferedWriter debugWriter;
    private String jobName;
    private UpCancellationSignal cancellationSignal;

    private boolean isCompleted = false;
    private UploadTaskGroup currentTaskGroup;
    private List<UploadTaskGroup> taskGroups = new ArrayList<>();

    public UploadJob(String jobName, Logger loggerParam, UpCancellationSignal cancellationSignal) {
        this.jobName = jobName;
        this.debugFile = Tools.tmpDir() + "/" + jobName + "_debug.log";
        try {
            this.debugWriter = new BufferedWriter(new FileWriter(this.debugFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logger = new Logger() {
            @Override
            public void log(String info) {
                appendDebugInfo(info);
                loggerParam.log(info);
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
            return;
        }
        logger.log(error);

        for (TestCase test : TestCase.testCases) {
            taskGroups.add(new UploadTaskGroup(test, jobName, logger, cancellationSignal));
        }
    }

    public void releaseResource() {
        if (debugWriter != null) {
            try {
                debugWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void prepare() {
        setCompleted(false);
        for (UploadTaskGroup group : taskGroups) {
            group.prepare();
        }
    }

    public void run() {
        prepare();

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (UploadTaskGroup group : taskGroups) {
                    if (cancellationSignal != null && cancellationSignal.isCancelled()) {
                        break;
                    }
                    currentTaskGroup = group;
                    group.run();
                    saveTasksToLocal();
                }
                setCompleted(true);
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

    public synchronized boolean isCompleted() {
        return isCompleted;
    }

    private synchronized void setCompleted(boolean completed) {
        isCompleted = completed;
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
    private String loadTasksFromLocal() {
        byte[] data = Cache.getCacheData(JobCacheName);
        if (data == null) {
            return "warning: not find cache data for key:" + JobCacheName;
        }

        UploadJob job = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(new String(data));
            job = jobFromJson(jsonObject);
        } catch (JSONException e) {
            return e.toString();
        }

        if (job == null) {
            return "parse cache data error";
        }
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

        return Cache.cacheData(JobCacheName, jsonData);
    }

    public void clearJobCacheIfNeeded() {
        removeDebugInfoFromLocalIfNeeded();
        removeTasksInfoFromLocalIfNeeded();
    }

    private void removeTasksInfoFromLocalIfNeeded() {
        if (executedTaskCount() == taskCount()) {
            Cache.removeCache(JobCacheName);
        }
    }

    private void appendDebugInfo(String info) {
        if (debugWriter == null) {
            return;
        }
        try {
            debugWriter.write(info);
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
            return null;
        }
        return job;
    }
}
