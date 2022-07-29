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

    public static synchronized void loadLocalCase() {
        testCases = getTestCases();
    }

    private static TestCase[] getTestCases() {
        List<TestCase> caseList = new ArrayList<>();

        int repeat = 10;
        int smallCaseFileCount = 10;
        int smallCaseConcurrentCount = 10;
        int midCaseFileCount = 10;
        int midCaseConcurrentCount = 10;
        int bigCaseRepeat = 5;
        int bigCaseFileCount = 6;
        int bigCaseConcurrentCount = 6;

//        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 2 * M, smallCaseConcurrentCount, false));
//            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 2 * M, smallCaseConcurrentCount, false));
//        }

        // 8K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 8 * KB, smallCaseConcurrentCount, false));
        }

        // 16K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 16 * KB, smallCaseConcurrentCount, false));
        }

        // 32K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 32 * KB, smallCaseConcurrentCount, false));
        }

        // 64K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 64 * KB, smallCaseConcurrentCount, false));
        }

        // 128K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 128 * KB, smallCaseConcurrentCount, false));
        }

        // 256K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 256 * KB, smallCaseConcurrentCount, false));
        }

        // 512K
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, 512 * KB, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, 512 * KB, smallCaseConcurrentCount, false));
        }

        // 1M
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, smallCaseFileCount, M, smallCaseConcurrentCount, false));
            caseList.add(new TestCase(UploadTask.TypeHttp3, smallCaseFileCount, M, smallCaseConcurrentCount, false));
        }

        // 2M
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 2 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 2 * M, midCaseConcurrentCount, false));
        }

        // 4M
        for (int i = 0; i < repeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 4 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 4 * M, midCaseConcurrentCount, false));
        }

        // 6M
        for (int i = 0; i < bigCaseRepeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 6 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 6 * M, midCaseConcurrentCount, false));
        }

        // 8M
        for (int i = 0; i < bigCaseRepeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, midCaseFileCount, 8 * M, midCaseConcurrentCount, true));
            caseList.add(new TestCase(UploadTask.TypeHttp3, midCaseFileCount, 8 * M, midCaseConcurrentCount, false));
        }
//
//        // 50M
//        for (int i = 0; i < bigCaseRepeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, bigCaseFileCount, 50 * M, bigCaseConcurrentCount, true));
//            caseList.add(new TestCase(UploadTask.TypeHttp3, bigCaseFileCount, 50 * M, bigCaseConcurrentCount, true));
//        }
//
//        // 100M
//        for (int i = 0; i < bigCaseRepeat; i++) {
//            caseList.add(new TestCase(UploadTask.TypeHttp2, bigCaseFileCount, 100 * M, bigCaseConcurrentCount, true));
//            caseList.add(new TestCase(UploadTask.TypeHttp3, bigCaseFileCount, 100 * M, bigCaseConcurrentCount, true));
//        }

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
            boolean disableHttp2 = testCaseJson.optBoolean("disable_http2", false);
            boolean disableHttp3 = testCaseJson.optBoolean("disable_http2", false);

            for (int r = 0; r < repeat; r++) {
                if (!disableHttp2) {
                    testCaseList.add(new TestCase(UploadTask.TypeHttp2, fileCount, fileSize, concurrentCount, isResumeV2));
                }
                if (!disableHttp3) {
                    testCaseList.add(new TestCase(UploadTask.TypeHttp3, fileCount, fileSize, concurrentCount, isResumeV2));
                }
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
        String requestType = "http2";
        if (this.requestType == UploadTask.TypeHttp3) {
            requestType = "http3";
        }
        return "data_" + jobName + "_" + Tools.getMemoryDesc(fileSize) + "_"  + requestType;
    }

    public interface Complete {
        void complete(String error);
    }
}
