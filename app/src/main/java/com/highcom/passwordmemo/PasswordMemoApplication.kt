package com.highcom.passwordmemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp

/**
 * パスワードメモアプリ用Applicationクラス
 * * データベースやリポジトリの初期化処理を行う
 *
 */
@HiltAndroidApp
class PasswordMemoApplication : Application() {
    companion object {
        lateinit var instance: PasswordMemoApplication
    }
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
        instance = this
    }
}