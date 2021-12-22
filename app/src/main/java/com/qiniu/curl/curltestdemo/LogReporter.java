package com.qiniu.curl.curltestdemo;

public class LogReporter {

    public static void reportUploadJobInfo(UploadJob job, Complete complete) {

    }

    public interface Complete {
        void complete(boolean isSuccess);
    }
}
