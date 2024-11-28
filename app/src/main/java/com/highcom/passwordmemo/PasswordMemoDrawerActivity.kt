package com.highcom.passwordmemo

import android.os.Bundle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.highcom.passwordmemo.databinding.ActivityPasswordMemoDrawerBinding
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ドロワービューレイアウトのアクティビティ
 *
 */
@AndroidEntryPoint
class PasswordMemoDrawerActivity : AppCompatActivity() {
    /** ドロワービューレイアウトのアクティビティbinding */
    private lateinit var binding: ActivityPasswordMemoDrawerBinding
    /** Firebase解析 */
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** ナビゲーションドロワー */
    lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)
        binding = ActivityPasswordMemoDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarPasswordMemoDrawer.toolbar)
        drawerLayout = binding.drawerLayout
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
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