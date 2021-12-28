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
        // 64K
        for (int i = 0; i < 10; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 50, 64 * KB, 16, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 50, 64 * KB, 16, false));
        }

        // 128K
        for (int i = 0; i < 10; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 40, 128 * KB, 16, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 40, 128 * KB, 16, false));
        }

        // 256K
        for (int i = 0; i < 10; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 20, 256 * KB, 16, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 20, 256 * KB, 16, false));
        }

        // 5M
        for (int i = 0; i < 4; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, 1, 5 * M, 16, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 1, 5 * M, 16, true));
            caseList.add(new TestCase(UploadTask.TypeHttp2, 1, 5 * M, 16, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, 1, 5 * M, 16, false));
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
