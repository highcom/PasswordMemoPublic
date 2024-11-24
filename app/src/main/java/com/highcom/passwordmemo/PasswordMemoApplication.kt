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
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
    }
}