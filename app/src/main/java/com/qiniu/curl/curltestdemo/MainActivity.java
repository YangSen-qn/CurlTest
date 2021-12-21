package com.qiniu.curl.curltestdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.utils.AsyncRun;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements Logger, UpCancellationSignal {
    public static final int StatusWaiting = 10;
    public static final int StatusUploading = 11;
    public static final int StatusCancelling = 12;

    private Button uploadBtn;
    private TextView taskInfo;
    private TextView taskCountText;
    private TextView taskExecutedCountText;

    private Timer refreshTimer;
    private String logInfo = "";
    private int status = StatusWaiting;
    private UploadJob job;

    @SuppressLint("CutPasteId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskInfo = findViewById(R.id.main_progress_info);
        taskInfo.setMovementMethod(ScrollingMovementMethod.getInstance());
        taskCountText = findViewById(R.id.main_progress_task_count);
        taskExecutedCountText = findViewById(R.id.main_progress_task_executed);

        uploadBtn = findViewById(R.id.upload_btn);
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadBtnAction();
            }
        });
    }

    private void uploadBtnAction() {
        if (job == null) {
            job = new UploadJob("", this, this);
        }

        if (status == StatusWaiting) {
            status = StatusUploading;
            updateStatus();
            startRefreshTimer();
            job.run();
        } else if (status == StatusUploading) {
            status = StatusCancelling;
            updateStatus();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatus() {
        if (job == null) {
            return;
        }

        if (job.isCompleted()) {
            status = StatusWaiting;
            stopRefreshTimer();
        }

        if (status == StatusWaiting) {
            uploadBtn.setText("上传");
        } else if (status == StatusUploading) {
            uploadBtn.setText("上传中...");
        } else if (status == StatusCancelling) {
            uploadBtn.setText("取消中...");
        }

        taskCountText.setText("" + job.taskCount());
        taskExecutedCountText.setText("" + job.executedTaskCount());
        taskInfo.setText(logInfo);
    }

    @Override
    public void log(String info) {
        logInfo += info + "\n";
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
        }, 0, 1000);
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}