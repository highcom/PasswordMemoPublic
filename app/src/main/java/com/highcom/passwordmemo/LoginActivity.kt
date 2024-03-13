package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.analytics.FirebaseAnalytics
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import com.highcom.passwordmemo.util.login.LoginService

class LoginActivity : AppCompatActivity(), View.OnClickListener {
    private var loginDataManager: LoginDataManager? = null
    var loginService: LoginService? = null
    var naviText: TextView? = null
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    private val passwordListViewModel: PasswordListViewModel by viewModels {
        PasswordListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_login)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        loginDataManager = LoginDataManager.Companion.getInstance(this)
        loginService = LoginService(loginDataManager, passwordListViewModel)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val versionText = findViewById<TextView>(R.id.versionText)
        try {
            val info = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            versionText.text = String.format("%s %s", getString(R.string.version), info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        naviText = findViewById<View>(R.id.navigateText) as TextView
        val loginbtn = findViewById<View>(R.id.loginButton) as Button
        loginbtn.setOnClickListener(this)
        val biometricloginbtn = findViewById<View>(R.id.biometricLoginButton) as ImageButton
        biometricloginbtn.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.loginButton -> {
                val editPassword =
                    (findViewById<View>(R.id.editMasterPassword) as TextInputEditText).text.toString()
                val naviMessage = loginService!!.passwordLogin(this, editPassword)
                if (naviMessage != null) naviText!!.text = naviMessage
            }

            R.id.biometricLoginButton -> loginService!!.biometricLogin(this)
            else -> {}
        }
        (findViewById<View>(R.id.editMasterPassword) as EditText).editableText.clear()
        loginDataManager!!.updateSetting()
        checkBiometricSetting()
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        loginDataManager!!.updateSetting()
        checkBiometricSetting()
        loginService!!.clearAnimation()
        if (loginDataManager!!.isMasterPasswordCreated) {
            naviText!!.setText(R.string.input_password)
        } else {
            naviText!!.setText(R.string.new_password)
        }

        // 背景色を設定する
        (findViewById<View>(R.id.loginView) as ConstraintLayout).setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
        // 入力内容は一旦クリアする
        (findViewById<View>(R.id.editMasterPassword) as EditText).editableText.clear()
        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    override fun onResume() {
        super.onResume()
        loginDataManager!!.updateSetting()
        loginService!!.clearFirstEditPassword()
        checkBiometricSetting()
        loginService!!.clearAnimation()
        if (loginDataManager!!.isMasterPasswordCreated) {
            naviText!!.setText(R.string.input_password)
        } else {
            naviText!!.setText(R.string.new_password)
        }
    }

    private fun checkBiometricSetting() {
        if (!loginDataManager!!.biometricLoginSwitchEnable) {
            findViewById<View>(R.id.biometricLoginButton).visibility = View.INVISIBLE
            return
        } else if (!loginDataManager!!.isMasterPasswordCreated) {
            findViewById<View>(R.id.biometricLoginButton).visibility = View.VISIBLE
            findViewById<View>(R.id.biometricLoginButton).isEnabled = false
            (findViewById<View>(R.id.biometricLoginButton) as ImageButton).setColorFilter(
                Color.parseColor(
                    "#CCCCCC"
                )
            )
            return
        }
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                findViewById<View>(R.id.biometricLoginButton).visibility = View.VISIBLE
                findViewById<View>(R.id.biometricLoginButton).isEnabled = true
                (findViewById<View>(R.id.biometricLoginButton) as ImageButton).setColorFilter(
                    Color.parseColor(
                        "#007AFF"
                    )
                )
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> findViewById<View>(R.id.biometricLoginButton).visibility =
                View.INVISIBLE

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                findViewById<View>(R.id.biometricLoginButton).visibility = View.VISIBLE
                findViewById<View>(R.id.biometricLoginButton).isEnabled = false
                (findViewById<View>(R.id.biometricLoginButton) as ImageButton).setColorFilter(
                    Color.parseColor(
                        "#CCCCCC"
                    )
                )
            }
        }
    }

    private fun setTextSize(size: Float) {
        (findViewById<View>(R.id.navigateText) as TextView).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.editMasterPassword) as EditText).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        (findViewById<View>(R.id.loginButton) as Button).setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
    }
}