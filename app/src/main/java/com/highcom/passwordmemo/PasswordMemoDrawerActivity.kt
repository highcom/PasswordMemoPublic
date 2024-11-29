package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    private lateinit var drawerLayout: DrawerLayout
    /** ツールバー */
    private lateinit var toolBar: Toolbar
    /** ドロワー制御トグル */
    lateinit var toggle: ActionBarDrawerToggle

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(R.style.AppTheme)
        binding = ActivityPasswordMemoDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarPasswordMemoDrawer.toolbar)
        // Drawer Layoutの設定
        drawerLayout = binding.drawerLayout
        toolBar = binding.appBarPasswordMemoDrawer.toolbar
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toggle = ActionBarDrawerToggle(this, drawerLayout,
            binding.appBarPasswordMemoDrawer.toolbar,
            R.string.drawer_open, R.string.drawer_close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

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

    /**
     * 全てのメニューを無効化
     *
     */
    fun allMenuDisabled() {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        toggle.isDrawerIndicatorEnabled = false
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    /**
     * ドロワーメニューを有効化
     *
     */
    fun drawerMenuEnabled() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.isDrawerIndicatorEnabled = true
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }

    /**
     * ドロワーメニューを無効化
     *
     */
    fun drawerMenuDisabled() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toggle.isDrawerIndicatorEnabled = false
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        toolBar.setNavigationIcon(R.drawable.ic_baseline_arrow_back_24)
    }

    override fun onDestroy() {
        super.onDestroy()
        //バックグラウンドの場合、Activityを破棄してログイン画面に戻る
        if (loginDataManager.displayBackgroundSwitchEnable && PasswordMemoLifecycle.isBackground) {
            finishAffinity()
        }
    }
}