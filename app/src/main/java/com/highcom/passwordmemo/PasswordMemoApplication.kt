package com.highcom.passwordmemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner

class PasswordMemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
    }
}