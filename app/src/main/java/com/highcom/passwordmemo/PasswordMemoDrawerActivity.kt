package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.firebase.analytics.FirebaseAnalytics
import com.highcom.passwordmemo.databinding.ActivityPasswordMemoDrawerBinding
import com.highcom.passwordmemo.domain.DarkModeUtil
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
    /** ツールバー */
    private lateinit var toolBar: Toolbar
    /** ドロワー制御トグル */
    lateinit var toggle: ActionBarDrawerToggle
    /** ログアウトボタン */
    lateinit var logoutButton: Button
    /** グループ一覧 */
    lateinit var drawerGroupList: ListView

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ダークモード設定を適用
        DarkModeUtil.applyDarkMode(this, loginDataManager.darkMode)

        // テーマを設定（ダークモード設定に基づいて適切なテーマを選択）
        val themeResId = if (DarkModeUtil.isDarkModeEnabled(this, loginDataManager.darkMode)) {
            R.style.AppTheme_Dark
        } else {
            R.style.AppTheme
        }
        setTheme(themeResId)
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
        // ログアウトボタン
        logoutButton = binding.navHeader.logoutButton
        // ヘッダータイトルにバージョン番号を追加
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            binding.navHeader.drawerTitle.text = getString(R.string.app_name) + " " + info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        // グループ一覧
        drawerGroupList = binding.groupListViewInsideNav
        // 背景色を設定する
        binding.drawerListView.setBackgroundColor(loginDataManager.backgroundColor)

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
     * ドロワーの背景色を設定する
     *
     * @param color 設定する背景色
     */
    fun setBackgroundColor(color: Int) {
        binding.drawerListView.setBackgroundColor(color)
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