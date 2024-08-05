@file:Suppress("DEPRECATION")

package com.highcom.passwordmemo

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class PasswordMemoLifecycle : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        isBackground = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        isBackground = true
    }

    companion object {
        var isBackground = false
            private set
    }
}