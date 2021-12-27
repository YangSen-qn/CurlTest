package com.qiniu.curl.curltestdemo;

import android.app.Application;

import com.pgyersdk.crash.PgyCrashManager;

public class QNApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PgyCrashManager.register(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        PgyCrashManager.unregister();
    }
}
