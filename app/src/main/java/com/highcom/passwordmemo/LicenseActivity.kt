package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.highcom.passwordmemo.util.login.LoginDataManager

/**
 * ライセンス画面アクティビティ
 *
 */
class LicenseActivity : AppCompatActivity() {
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        title = getString(R.string.license)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loginDataManager = LoginDataManager.getInstance(application)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        (findViewById<View>(R.id.licenseView) as LinearLayout).setBackgroundColor(
            LoginDataManager.getInstance(application)!!.backgroundColor
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    public override fun onDestroy() {
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }
}