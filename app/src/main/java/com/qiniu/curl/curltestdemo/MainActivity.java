package com.qiniu.curl.curltestdemo;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.utils.AsyncRun;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements Logger, UpCancellationSignal {
    public static final int StatusWaiting = 10;
    public static final int StatusUploading = 11;
    public static final int StatusCancelling = 12;
    public static final int StatusUploadLog = 13;
    public static final int StatusUploadingLog = 14;

    private static final String defaultAlert = DefaultAlert();
    private static String DefaultAlert() {
        String alert = "操作步骤：\n";
        alert += "1. 【输入上传标识】上传标识为上传进度缓存的 id ；在点击 [开始任务] 时程序会根据此 id 加载缓存的上传进度；在上传过程中不允许修改此 id 。\n";
        alert += "2. 【等待上传任务结束】此过程耗时较长；进度中会展示任务的状态；上传过程中会缓存状态，因此上传过程中可以暂停任务。\n";
        alert += "3. 【上传日志】当按钮变为 [上传日志] 则表明上传结束，可进行日志上传；点击后按钮显示 [日志上传中...] ，此过程不可取消。\n";
        alert += "4. 【任务完成】当按钮重新变为 [开始任务] 则表示任务已经完成。\n";
        alert += "\n";
        alert += "注：\n";
        alert += "   在任务的任何状态均可杀死 App, 下次打开 App 在输入上传标识并点击[上传]按钮后，会根据之前的君度继续进行。\n\n";
        return alert;
    }

    private EditText jobIdET;
    private ProgressBar currentTaskProgressPB;
    private Button uploadBtn;
    private TextView taskInfoTV;
    private TextView taskCountTV;
    private TextView taskExecutedCountTV;

    private Timer refreshTimer;
    private String logInfo = "";
    private int status = StatusWaiting;
    private UploadJob job;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        jobIdET = findViewById(R.id.main_upload_id);
        jobIdET.setHint("上传标识会为上传任务进度缓存的 id");
        currentTaskProgressPB = findViewById(R.id.main_current_task_progress);
        currentTaskProgressPB.setMax(100);
        taskInfoTV = findViewById(R.id.main_progress_info);
        taskInfoTV.setMovementMethod(ScrollingMovementMethod.getInstance());
        taskInfoTV.setText(defaultAlert);
        taskCountTV = findViewById(R.id.main_progress_task_count);
        taskExecutedCountTV = findViewById(R.id.main_progress_task_executed);

        uploadBtn = findViewById(R.id.upload_btn);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadBtnAction();
            }
        });
    }

    private void uploadBtnAction() {
        String jobName = jobIdET.getText().toString();
        if (jobName.length() == 0) {
            alert("请输入上传标识");
            return;
        }

        if (job == null || !job.getJobName().equals(jobName)) {
            taskInfoTV.setText(defaultAlert);
            job = new UploadJob(jobName, this, this);
        }

        if (status == StatusWaiting) {
            status = StatusUploading;
            startRefreshTimer();
            job.run();
        } else if (status == StatusUploading) {
            status = StatusCancelling;
            updateStatus();
        } else if (status == StatusUploadLog) {
            status = StatusUploadingLog;
            updateStatus();
            LogReporter.reportUploadJob(job, new LogReporter.Complete() {
                @Override
                public void complete(boolean isSuccess) {
                    if (isSuccess) {
                        status = StatusWaiting;
                        job.clearJobCacheIfNeeded();
                        updateStatus();
                        job = null;
                        log(false, "日志上传成功 \n");
                        log(false, "😁😁😁 恭喜您上传成功 😁😁😁\n");
                    } else {
                        status = StatusUploadLog;
                    }
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatus() {
        UploadJob job = this.job;

        int taskCount = 0;
        int executedTaskCount = 0;
        int currentTaskProgress = 0;
        if (job != null) {
            taskCount = job.taskCount();
            executedTaskCount = job.executedTaskCount();
            UploadTaskGroup currentTask = job.currentTask();
            if (currentTask != null) {
                currentTaskProgress = (int)(currentTask.progress() * 100);
            }
        }

        taskCountTV.setText("" + taskCount);
        taskExecutedCountTV.setText("" + executedTaskCount);
        currentTaskProgressPB.setProgress(currentTaskProgress);
        synchronized (this) {
            if (logInfo.length() > 0) {
                taskInfoTV.append(logInfo);
                logInfo = "";
            }
        }

        if (job == null) {
            return;
        }

        if (job.isCompleted()) {
            if (job.taskCount() == job.executedTaskCount()) {
                if (status == StatusUploading) {
                    status = StatusUploadLog;
                }
            } else {
                status = StatusWaiting;
                stopRefreshTimer();
            }
        }

        if (status == StatusWaiting) {
            jobIdET.setEnabled(true);
            uploadBtn.setText("开始任务");
        } else if (status == StatusUploading) {
            jobIdET.setEnabled(false);
            uploadBtn.setText("暂停任务");
        } else if (status == StatusCancelling) {
            uploadBtn.setText("任务取消中...");
        } else if (status == StatusUploadLog) {
            uploadBtn.setText("上传日志");
        } else if (status == StatusUploadingLog) {
            uploadBtn.setText("日志上传中...");
        }
    }

    @Override
    public void log(boolean isDetail, String info) {
        if (isDetail) {
            return;
        }

        synchronized (this) {
            logInfo += info ;
        }
    }

    @Override
    public boolean isCancelled() {
        return status == StatusCancelling;
    }

    private void startRefreshTimer() {
        if (refreshTimer == null) {
            refreshTimer = new Timer("refresh timer");
        }

        refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                });
            }
        }, 0, 500);
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer.purge();
            refreshTimer = null;
        }
    }


    // alert
    private void alert(String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}