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
        jobIdET.setHint("上传标识会作为进度缓存的标识，上传过程中不可编辑。");
        currentTaskProgressPB = findViewById(R.id.main_current_task_progress);
        currentTaskProgressPB.setMax(100);
        taskInfoTV = findViewById(R.id.main_progress_info);
        taskInfoTV.setMovementMethod(ScrollingMovementMethod.getInstance());
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

        if (job == null) {
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
            LogReporter.reportUploadJobInfo(job, new LogReporter.Complete() {
                @Override
                public void complete(boolean isSuccess) {
                    if (isSuccess) {
                        job.clearJobCacheIfNeeded();
                        job.releaseResource();
                        job = null;
                    }
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatus() {
        UploadJob job = this.job;
        if (job == null) {
            return;
        }

        if (job.isCompleted()) {
            if (job.taskCount() == job.executedTaskCount()) {
                status = StatusUploadLog;
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
            uploadBtn.setText("任务执行中...");
        } else if (status == StatusCancelling) {
            uploadBtn.setText("任务取消中...");
        } else if (status == StatusUploadLog) {
            uploadBtn.setText("上传日志");
        } else if (status == StatusUploadingLog) {
            uploadBtn.setText("日志上传中...");
        }

        taskCountTV.setText("" + job.taskCount());
        taskExecutedCountTV.setText("" + job.executedTaskCount());
        UploadTaskGroup currentTask = job.currentTask();
        if (currentTask != null) {
            currentTaskProgressPB.setProgress((int)(currentTask.progress() * 100));
        } else {
            currentTaskProgressPB.setProgress(0);
        }

        synchronized (this) {
            if (logInfo.length() > 0) {
                taskInfoTV.append(logInfo);
                logInfo = "";
            }
        }
    }

    @Override
    public void log(String info) {
        synchronized (this) {
            logInfo += info + "\n";
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


    // 上传日志
    private void uploadLog() {

    }
}