package com.qiniu.curl.curltestdemo;

import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.utils.Wait;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UploadTaskGroup {

    private Logger logger;
    private boolean isResumeV2;
    private int requestType;
    private long fileCount;
    private long fileSize;
    private String taskName; // xxx_512k
    private int concurrentCount;
    private UpCancellationSignal cancellationSignal;
    private List<UploadTask> tasks = new ArrayList<>();

    private int completeWorkCount = 0;
    private boolean isRunning = false;

    public UploadTaskGroup(TestCase testCase, String jobName, Logger logger, UpCancellationSignal cancellationSignal) {
        this.requestType = testCase.requestType;
        this.isResumeV2 = testCase.isResumeV2;
        this.fileCount = testCase.fileCount;
        this.fileSize = testCase.fileSize;
        this.taskName = testCase.getCaseName(jobName);
        this.concurrentCount = testCase.concurrentCount;
        this.logger = logger;
        this.cancellationSignal = cancellationSignal;
        createTasks();
    }

    private UploadTaskGroup() {
    }

    private void createTasks() {
        for (int i = 0; i < fileCount; i++) {
            String key = taskName + "_" + i;
            tasks.add(new UploadTask(requestType, fileSize, key, isResumeV2, logger, cancellationSignal));
        }
    }

    public double progress() {
        int completedCount = 0;
        for (UploadTask task : tasks) {
            if (task.isComplete()) {
                completedCount += 1;
            }
        }
        return (double) completedCount / fileCount;
    }

    public boolean isCompleted() {
        boolean completed = true;
        for (UploadTask task : tasks) {
            if (!task.isComplete()) {
                completed = false;
                break;
            }
        }
        return completed;
    }

    public int successCount() {
        int count = 0;
        for (UploadTask task : tasks) {
            if (task.isComplete() && task.isSuccess()) {
                count += 1;
            }
        }
        return count;
    }

    public void prepare() {
        for (UploadTask task : tasks) {
            task.prepare();
        }
    }

    public void run() {
        if (isCompleted()) {
            return;
        }

        synchronized (this) {
            if (isRunning) {
                return;
            }
            isRunning = true;
        }

        logger.log(false, "task group start:" + taskName);
        completeWorkCount = 0;
        Wait wait = new Wait();
        for (int i = 0; i < concurrentCount; i++) {
            final int index = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doUpload(index);

                    synchronized (wait) {
                        completeWorkCount += 1;
                        if (completeWorkCount == concurrentCount) {
                            wait.stopWait();
                        }
                    }
                }
            }).start();
        }
        wait.startWait();
        logger.log(false, description());
        logger.log(false, "task group end:" + taskName + "\n");

        synchronized (this) {
            isRunning = false;
        }
    }

    private void doUpload(int workIndex) {
        UploadTask task = null;
        do {
            task = getNextNeedUploadTask();
            if (task != null) {
                task.run();
            }

            if (cancellationSignal != null && cancellationSignal.isCancelled()) {
                logger.log(false, "task cancelled and work stop:" + workIndex);
                break;
            }
        } while (task != null);
    }

    private synchronized UploadTask getNextNeedUploadTask() {
        UploadTask task = null;
        for (UploadTask t : tasks) {
            if (t.needUpload()) {
                t.setStatus(UploadTask.StatusUploading);
                task = t;
                break;
            }
        }
        return task;
    }

    private String description() {
        String desc = "";
        desc += taskName;
        desc += " ResumeV2:" + isResumeV2;
        desc += requestType == UploadTask.TypeHttp2 ? " http2 " : " http3";
        desc += " ConcurrentCount:" + concurrentCount;
        desc += " FileCount:" + fileCount;
        desc += " SuccessCount:" + successCount();
        return desc;
    }

    public String reportInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("task_name", taskName);
            jsonObject.putOpt("request_type", requestType == UploadTask.TypeHttp3 ? "http3" : "http2");
            jsonObject.putOpt("file_count", fileCount);
            jsonObject.putOpt("file_size", fileSize);
            jsonObject.putOpt("concurrent_count", concurrentCount);
            long duration = 0;
            long successCount = 0;
            for (UploadTask task : tasks) {
                if (task.isComplete() && task.isSuccess()) {
                    duration += task.getDuration();
                    successCount += 1;
                }
            }
            jsonObject.putOpt("success_count", successCount);
            jsonObject.putOpt("average_duration", successCount > 0 ? duration / successCount : 0);
        } catch (JSONException e) {
            jsonObject = new JSONObject();
        }
        return jsonObject.toString();
    }

    //----------- json ------------
    public JSONObject toJsonData() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("task_name", taskName);
            jsonObject.putOpt("request_type", requestType);
            jsonObject.putOpt("file_count", fileCount);
            jsonObject.putOpt("file_size", fileSize);
            jsonObject.putOpt("concurrent_count", concurrentCount);
            JSONArray taskJsonArray = new JSONArray();
            for (UploadTask task : tasks) {
                JSONObject taskJson = task.toJsonData();
                if (taskJson == null) {
                    return null;
                }
                taskJsonArray.put(taskJson);
            }
            jsonObject.putOpt("tasks", taskJsonArray);
        } catch (JSONException e) {
            jsonObject = null;
        }
        return jsonObject;
    }

    public static UploadTaskGroup taskGroupFromJson(JSONObject jsonObject, Logger logger, UpCancellationSignal cancellationSignal) {
        UploadTaskGroup taskGroup = new UploadTaskGroup();
        taskGroup.logger = logger;
        taskGroup.cancellationSignal = cancellationSignal;
        try {
            taskGroup.taskName = jsonObject.getString("task_name");
            taskGroup.requestType = jsonObject.getInt("request_type");
            taskGroup.fileCount = jsonObject.getInt("file_count");
            taskGroup.fileSize = jsonObject.getLong("file_size");
            taskGroup.concurrentCount = jsonObject.getInt("concurrent_count");
            taskGroup.tasks = new ArrayList<>();
            JSONArray taskJsonArray = jsonObject.getJSONArray("tasks");
            for (int i = 0; i < taskJsonArray.length(); i++) {
                UploadTask task = UploadTask.taskFromJson(taskJsonArray.getJSONObject(i), logger, cancellationSignal);
                if (task == null) {
                    return null;
                }
                taskGroup.tasks.add(task);
            }
        } catch (JSONException e) {
            return null;
        }
        return taskGroup;
    }
}
