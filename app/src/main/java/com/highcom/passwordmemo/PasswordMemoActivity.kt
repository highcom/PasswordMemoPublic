package com.highcom.passwordmemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * ログイン画面アクティビティ
 *
 */
class PasswordMemoActivity : AppCompatActivity() {
    /** Firebase解析 */
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_password_memo)
        // Firebaseの設定
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        //バックグラウンドの場合、Activityを破棄してログイン画面に戻る
        if ((application as PasswordMemoApplication).loginDataManager.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            finishAffinity()
        }
    }
}