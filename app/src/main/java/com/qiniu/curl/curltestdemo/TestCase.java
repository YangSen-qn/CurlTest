package com.qiniu.curl.curltestdemo;

public class TestCase {

    public static final TestCase[] testCases = new TestCase[]{
            new TestCase(UploadTask.TypeHttp2, 100, 512, 16),
    };

    public final int requestType;
    public final long fileCount;
    public final long fileSize;
    public final int concurrentCount;

    public TestCase(int requestType, long fileCount, long fileSize, int concurrentCount) {
        this.requestType = requestType;
        this.fileCount = fileCount;
        this.fileSize = fileSize;
        this.concurrentCount = concurrentCount;
    }

    public String getCaseName(String jobName) {
        return jobName + "_" + Tools.getMemoryDesc(fileSize);
    }
}
