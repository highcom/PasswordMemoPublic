package com.highcom.passwordmemo;

import android.app.Application;

import androidx.lifecycle.ProcessLifecycleOwner;

public class PasswordMemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new PasswordMemoLifecycle());
    }
}
