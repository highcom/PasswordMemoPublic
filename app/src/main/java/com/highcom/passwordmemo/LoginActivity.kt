package com.highcom.passwordmemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * ログイン画面アクティビティ
 *
 */
class LoginActivity : AppCompatActivity() {
    /** Firebase解析 */
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_login)
        // Firebaseの設定
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }
}