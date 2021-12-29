package com.qiniu.curl.curltestdemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestCase {
    public static final long KB = 1;
    public static final long M = 1024;
    public static final TestCase[] testCases = getTestCases();

    private static TestCase[] getTestCases() {
        List<TestCase> caseList = new ArrayList<>();

        // 8K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 8 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 8 * KB, 6, false));
        }

        // 16K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 16 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 16 * KB, 6, false));
        }

        // 32K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 32 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 32 * KB, 6, false));
        }

        // 64K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 64 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 64 * KB, 6, false));
        }

        // 128K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 128 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 128 * KB, 6, false));
        }

        // 256K
        for (int i = 0; i < 20; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 10, 256 * KB, 6, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 10, 256 * KB, 6, false));
        }

        // 5M
        for (int i = 0; i < 4; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 1, 5 * M, 2, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 1, 5 * M, 2, true));
            caseList.add(new TestCase(UploadTask.TypeHttp2, 1, 5 * M, 2, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 1, 5 * M, 2, false));
        }

        return caseList.toArray(new TestCase[0]);
    }

    public final int requestType;
    public final long fileCount;
    public final long fileSize;
    public final int concurrentCount;
    public final boolean isResumeV2;

    public TestCase(int requestType, long fileCount, long fileSize, int concurrentCount, boolean isResumeV2) {
        this.requestType = requestType;
        this.fileCount = fileCount;
        this.fileSize = fileSize;
        this.concurrentCount = concurrentCount;
        this.isResumeV2 = isResumeV2;
    }

    public String getCaseName(String jobName) {
        return "data_" + jobName + "_" + Tools.getMemoryDesc(fileSize);
    }
}
