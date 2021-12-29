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
        int smallCaseFileCount = 10;
        int smallCaseConcurrentCount = 10;
        // 8K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
        }

        // 16K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
        }

        // 32K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
        }

        // 64K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
        }

        // 128K
        for (int i = 0; i < 50; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
        }

        // 256K
        for (int i = 0; i < 20; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
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
