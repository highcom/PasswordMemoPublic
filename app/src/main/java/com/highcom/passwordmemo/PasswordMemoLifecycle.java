package com.highcom.passwordmemo;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.Lifecycle;

public class PasswordMemoLifecycle implements LifecycleObserver {
    private static boolean isBackground = false;

    public static boolean getIsBackground() {
        return isBackground;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        isBackground = false;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        isBackground = true;
    }
}
