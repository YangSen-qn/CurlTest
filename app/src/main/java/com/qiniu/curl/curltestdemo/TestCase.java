package com.qiniu.curl.curltestdemo;

public class TestCase {
    public static final long KB = 1;
    public static final long M = 1024;
    public static final TestCase[] testCases = new TestCase[]{
            new TestCase(UploadTask.TypeHttp2, 100, 512 * KB, 16),
            new TestCase(UploadTask.TypeHttp3, 100, 512 * KB, 16),
            new TestCase(UploadTask.TypeHttp2, 100, 512 * KB, 16),
            new TestCase(UploadTask.TypeHttp3, 100, 512 * KB, 16),
            new TestCase(UploadTask.TypeHttp2, 100, 512 * KB, 16),
            new TestCase(UploadTask.TypeHttp3, 100, 512 * KB, 16),

            new TestCase(UploadTask.TypeHttp2, 10, 5 * M, 4),
            new TestCase(UploadTask.TypeHttp3, 10, 5 * M, 4),
            new TestCase(UploadTask.TypeHttp2, 10, 5 * M, 4),
            new TestCase(UploadTask.TypeHttp3, 10, 5 * M, 4),
            new TestCase(UploadTask.TypeHttp2, 10, 5 * M, 4),
            new TestCase(UploadTask.TypeHttp3, 10, 5 * M, 4),

            new TestCase(UploadTask.TypeHttp2, 2, 50 * M, 2),
            new TestCase(UploadTask.TypeHttp3, 2, 50 * M, 2),
            new TestCase(UploadTask.TypeHttp2, 2, 50 * M, 2),
            new TestCase(UploadTask.TypeHttp3, 2, 50 * M, 2),
            new TestCase(UploadTask.TypeHttp2, 2, 50 * M, 2),
            new TestCase(UploadTask.TypeHttp3, 2, 50 * M, 2),
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
