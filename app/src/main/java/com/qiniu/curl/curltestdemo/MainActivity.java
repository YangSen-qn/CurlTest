package com.qiniu.curl.curltestdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pgyersdk.activity.FeedbackActivity;
import com.pgyersdk.feedback.PgyFeedbackShakeManager;
import com.pgyersdk.javabean.AppBean;
import com.pgyersdk.update.PgyUpdateManager;
import com.pgyersdk.update.UpdateManagerListener;
import com.qiniu.android.storage.UpCancellationSignal;
import com.qiniu.android.utils.AsyncRun;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements Logger, UpCancellationSignal {
    public static final int StatusWaiting = 0;
    public static final int StatusGetTestCase = 10;
    public static final int StatusUpload = 20;
    public static final int StatusUploading = 21;
    public static final int StatusCancelling = 30;
    public static final int StatusUploadLog = 40;
    public static final int StatusUploadingLog = 50;

    private static final String defaultAlert = DefaultAlert();

    private static String DefaultAlert() {
        String alert = "操作步骤：\n";
        alert += "1. 【输入上传标识】上传标识为上传进度缓存的 id ；在点击 [开始任务] 时程序会根据此 id 加载当前手机内缓存的上传进度；在上传过程中不允许修改此 id 。\n";
        alert += "3. 【获取测试用例】从服务端获取测试用例，此过程不可取消。\n";
        alert += "3. 【等待上传任务结束】此过程耗时较长；进度中会展示任务的状态；上传过程中会缓存状态，因此上传过程中可以暂停任务。\n";
        alert += "4. 【上传日志】当按钮变为 [上传日志] 则表明上传结束，可进行日志上传；点击后按钮显示 [日志上传中...] ，此过程不可取消；正常情况下，日志会在上传任务结束后自动上传，当日志上传失败时需要手动点击[上传日志]进行重试。\n";
        alert += "5. 【任务完成】当按钮重新变为 [开始任务] 则表示此任务已经完成，此任务的缓存会从手机中清除；如需再执行任务重复步骤 1 ~ 4。\n";
        alert += "\n";
        alert += "注：\n";
        alert += "   想反馈？那就使劲摇您的手机吧，会有提示框！！！\n";
        alert += "   在任务的任何状态均可杀死 App, 下次打开 App 在输入上传标识并点击[上传]按钮后，会加载任务进度缓存并继续进行任务。\n\n";
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

        PgyUpdateManager.setIsForced(false); //设置是否强制更新。true为强制更新；false为不强制更新（默认值）。
        PgyUpdateManager.register(MainActivity.this,
                new UpdateManagerListener() {

                    @Override
                    public void onUpdateAvailable(final String result) {

                        // 将新版本信息封装到AppBean中
                        final AppBean appBean = getAppBeanFromString(result);
                        if (appBean.getVersionName().equals(Tools.getAppVersion(null))) {
                            return;
                        }

                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("更新")
                                .setMessage(appBean.getReleaseNote())
                                .setCancelable(true)
                                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Uri uri = Uri.parse("https://www.pgyer.com/o6kj");
                                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                        startActivity(intent);
                                    }
                                }).show();
                    }

                    @Override
                    public void onNoUpdateAvailable() {
                    }
                });

        PgyFeedbackShakeManager.setShakingThreshold(950); // 自定义摇一摇的灵敏度，默认为950，数值越小灵敏度越高。
        PgyFeedbackShakeManager.register(MainActivity.this); // 以对话框的形式弹出，对话框只支持竖屏
        // 以Activity的形式打开，这种情况下必须在AndroidManifest.xml配置FeedbackActivity
        FeedbackActivity.setBarImmersive(true); // 打开沉浸式,默认为false
        PgyFeedbackShakeManager.register(MainActivity.this, true); // 相当于使用Dialog的方式；

        Tools.context = this;

        Tools.keepScreenLongLight(this);

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

    @Override
    protected void onRestart() {
        super.onRestart();
        requestPermissions();
    }

    private void uploadBtnAction() {
        String jobName = jobIdET.getText().toString();
        if (jobName.length() == 0) {
            alert("请输入上传标识");
            return;
        }

        if (status == StatusWaiting) {
            startRefreshTimer();
            if (TestCase.testCases == null) {
                status = StatusGetTestCase;
                getTestCase();
            } else {
                status = StatusUpload;
            }
            updateStatus();
        } else if (status == StatusUploading) {
            status = StatusCancelling;
            updateStatus();
        } else if (status == StatusUploadLog) {
            status = StatusUploadingLog;
            updateStatus();
            uploadLog();
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateStatus() {
        UploadJob job = this.job;

        int taskCount = 0;
        int executedTaskCount = 0;
        int currentTaskProgress = 0;

        if (TestCase.testCases != null && status == StatusUpload) {
            status = StatusUploading;
            runTestCase();
        }

        if (job != null) {
            taskCount = job.taskCount();
            executedTaskCount = job.executedTaskCount();
            UploadTaskGroup currentTask = job.currentTask();
            if (currentTask != null) {
                currentTaskProgress = (int) (currentTask.progress() * 100);
            }

            if (job.isCompleted()) {
                if (job.taskCount() == job.executedTaskCount()) {
                    if (status == StatusUploading) {
                        status = StatusUploadingLog;
                        uploadLog();
                    }
                } else {
                    status = StatusWaiting;
                    stopRefreshTimer();
                }
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

        if (status == StatusWaiting) {
            jobIdET.setEnabled(true);
            uploadBtn.setText("开始任务");
        } else if (status == StatusGetTestCase) {
            jobIdET.setEnabled(false);
            uploadBtn.setText("获取测试用例...");
        } else if (status == StatusUpload) {
            jobIdET.setEnabled(false);
            uploadBtn.setText("开始上传");
        } else if (status == StatusUploading) {
            jobIdET.setEnabled(false);
            uploadBtn.setText("暂停上传");
        } else if (status == StatusCancelling) {
            uploadBtn.setText("任务暂停中...");
        } else if (status == StatusUploadLog) {
            uploadBtn.setText("上传日志");
        } else if (status == StatusUploadingLog) {
            uploadBtn.setText("日志上传中...");
        }
    }

    private void getTestCase() {
        TestCase.downloadTestCase(new TestCase.Complete() {
            @Override
            public void complete(String error) {
                AsyncRun.runInMain(new Runnable() {
                    @Override
                    public void run() {
                        if (error != null) {
                            alert("error:" + error);
                            status = StatusWaiting;
                            stopRefreshTimer();
                        } else {
                            status = StatusUpload;
                        }
                    }
                });
            }
        });
    }

    private void runTestCase() {
        String jobName = jobIdET.getText().toString();
        if (job == null || !job.getJobName().equals(jobName)) {
            taskInfoTV.setText(defaultAlert);
            job = new UploadJob(jobName, this, this);
        }
        job.run();
    }

    private void uploadLog() {
        LogReporter.reportUploadJob(job, new LogReporter.Complete() {
            @Override
            public void complete(boolean isSuccess) {
                if (isSuccess) {
                    status = StatusWaiting;
                    job.clearJobCacheIfNeeded();
                    updateStatus();
                    job = null;
                    log(false, "日志上传成功 \n");
                    log(false, "😁😁😁 完成任务啦 😁😁😁\n");
                } else {
                    status = StatusUploadLog;
                }
            }
        });
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //没有权限则申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void log(boolean isDetail, String info) {
        if (isDetail) {
            return;
        }

        synchronized (this) {
            logInfo += info;
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