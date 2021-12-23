package com.qiniu.curl.curltestdemo;

import java.io.File;

public class LogReporter {

    public static void reportUploadJob(UploadJob job, Complete complete) {
        // 1. 上报结果
        reportUploadJobInfo(job, new Complete() {
            @Override
            public void complete(boolean isSuccess) {
                if (!isSuccess) {
                    complete.complete(false);
                    return;
                }

                // 2. 上报 debug
                reportUploadJobDebug(job, new Complete() {
                    @Override
                    public void complete(boolean isSuccess) {
                        complete.complete(isSuccess);
                    }
                });
            }
        });
    }

    private static void reportUploadJobInfo(UploadJob job, Complete complete) {
        String key = "job_info_" + job.getJobName() + "_" + Tools.getFormatDate(job.getCreateTimestamp());
        Uploader.getInstance().uploadData(job.reportInfo(), key, new Uploader.Complete() {
            @Override
            public void complete(boolean isSuccess, String error) {
                if (isSuccess) {
                    job.setInfoUploaded(true);
                }
                complete.complete(isSuccess);
            }
        });
    }

    private static void reportUploadJobDebug(UploadJob job, Complete complete) {
        String debugFilePath = job.getDebugFilePath();
        if (debugFilePath == null || debugFilePath.length() == 0) {
            complete.complete(true);
            return;
        }

        File file = new File(debugFilePath);
        if (!file.exists() || !file.isFile() || file.length() == 0) {
            complete.complete(true);
            return;
        }

        String key = "job_debug_" + job.getJobName() + "_" + Tools.getFormatDate(job.getCreateTimestamp());
        Uploader.getInstance().uploadFile(debugFilePath, key, new Uploader.Complete() {
            @Override
            public void complete(boolean isSuccess, String error) {
                if (isSuccess) {
                    job.setDebugFileUploaded(true);
                }
                complete.complete(isSuccess);
            }
        });
    }

    public interface Complete {
        void complete(boolean isSuccess);
    }
}
