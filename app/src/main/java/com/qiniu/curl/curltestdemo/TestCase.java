package com.qiniu.curl.curltestdemo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TestCase {
    public static final long KB = 1;
    public static final long M = 1024;
    public static String testCasesVersion = null;
    public static TestCase[] testCases = null;

    private static TestCase[] getTestCases() {
        List<TestCase> caseList = new ArrayList<>();
        int repeat = 50;
        int smallCaseFileCount = 10;
        int smallCaseConcurrentCount = 10;
        int midCaseFileCount = 10;
        int midCaseConcurrentCount = 10;
        int bigCaseFileCount = 6;
        int bigCaseConcurrentCount = 6;

        // 8K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
        }

        // 16K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
        }

        // 32K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
        }

        // 64K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
        }

        // 128K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
        }

        // 256K
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
        }

        // 5M
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 5 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 5 * M, midCaseConcurrentCount, true));
        }

        // 10M
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 10 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 10 * M, midCaseConcurrentCount, true));
        }

        // 50M
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, bigCaseFileCount, 50 * M, bigCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, bigCaseFileCount, 50 * M, bigCaseConcurrentCount, true));
        }

        // 100M
        for (int i = 0; i < repeat; i++) {
            caseList.add(new TestCase(UploadTask.TypeHttp2, bigCaseFileCount, 100 * M, bigCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, bigCaseFileCount, 100 * M, bigCaseConcurrentCount, true));
        }

        return caseList.toArray(new TestCase[0]);
    }

    public static void downloadTestCase(Complete complete) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = "http://r4i68qquh.hd-bkt.clouddn.com/test_case.json";
                    Request request = new okhttp3.Request.Builder().get().url(urlString).build();
                    OkHttpClient client = new OkHttpClient();
                    Response response = client.newCall(request).execute();
                    String jsonString = "";
                    if (response.body() != null) {
                        jsonString = response.body().byteString().string(StandardCharsets.UTF_8);
                    }

                    if (jsonString.length() == 0) {
                        if (complete != null) {
                            complete.complete("can't get test case");
                        }
                    }
                    JSONObject jsonObject = new JSONObject(jsonString);
                    parseTestCaseFromJson(jsonObject);
                } catch (Exception e) {
                    e.printStackTrace();
                    if (complete != null) {
                        complete.complete(e.toString());
                    }
                    return;
                }

                if (complete != null) {
                    complete.complete(null);
                }
            }
        }).start();
    }

    private static void parseTestCaseFromJson(JSONObject jsonObject) throws Exception {
        testCasesVersion = jsonObject.optString("version");
        JSONArray testCaseJsonArray = jsonObject.getJSONArray("test_case");
        int size = testCaseJsonArray.length();
        List<TestCase> testCaseList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JSONObject testCaseJson = testCaseJsonArray.getJSONObject(i);
            if (testCaseJson == null) {
                continue;
            }

            int repeat = testCaseJson.getInt("repeat");
            int fileCount = testCaseJson.getInt("file_count");
            long fileSize = testCaseJson.getLong("file_size");
            int concurrentCount = testCaseJson.getInt("concurrent_count");
            boolean isResumeV2 = testCaseJson.optBoolean("is_resume_v2", true);
            for (int r = 0; r < repeat; r++) {
                testCaseList.add(new TestCase(UploadTask.TypeHttp2, fileCount, fileSize, concurrentCount, isResumeV2));
                testCaseList.add(new TestCase(UploadTask.TypeHttp3, fileCount, fileSize, concurrentCount, isResumeV2));
            }
        }

        testCases = testCaseList.toArray(new TestCase[0]);
    }

    public final int requestType;
    public final int fileCount;
    public final long fileSize;
    public final int concurrentCount;
    public final boolean isResumeV2;

    public TestCase(int requestType, int fileCount, long fileSize, int concurrentCount, boolean isResumeV2) {
        this.requestType = requestType;
        this.fileCount = fileCount;
        this.fileSize = fileSize;
        this.concurrentCount = concurrentCount;
        this.isResumeV2 = isResumeV2;
    }

    public String getCaseName(String jobName) {
        return "data_" + jobName + "_" + Tools.getMemoryDesc(fileSize);
    }

    public interface Complete {
        void complete(String error);
    }
}
