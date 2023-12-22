package com.highcom.passwordmemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase

class PasswordMemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
    }

    val database by lazy { PasswordMemoRoomDatabase.getDatabase(this) }
    val epository by lazy { PasswordMemoRepository(database.passwordDao(), database.groupDao()) }
}