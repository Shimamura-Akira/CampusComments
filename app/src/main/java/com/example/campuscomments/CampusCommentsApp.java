package com.example.campuscomments;

import android.app.Application;

import cn.bmob.v3.Bmob;

public class CampusCommentsApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Bmob.initialize(this, BuildConfig.BMOB_APP_ID);
    }
}
