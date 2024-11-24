package com.highcom.passwordmemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ログイン画面アクティビティ
 *
 */
@AndroidEntryPoint
class PasswordMemoActivity : AppCompatActivity() {
    /** Firebase解析 */
    private var mFirebaseAnalytics: FirebaseAnalytics? = null
    @Inject
    lateinit var loginDataManager: LoginDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_password_memo)
        // Firebaseの設定
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        // AdMobの初期設定
        MobileAds.initialize(applicationContext) { }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                listOf(
                    getString(R.string.admob_test_device),
                    getString(R.string.admob_test_device_xaomi)
                )
            ).build()
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        //バックグラウンドの場合、Activityを破棄してログイン画面に戻る
        if (loginDataManager.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            finishAffinity()
        }
    }
}