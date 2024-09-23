package com.highcom.passwordmemo.ui.fragment

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.biometric.BiometricManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.highcom.passwordmemo.PasswordListActivity
import com.highcom.passwordmemo.PasswordMemoApplication
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.viewmodel.LoginViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.launch

/**
 * ログイン画面フラグメント
 *
 */
class LoginFragment : Fragment(), View.OnClickListener {

    /** ログイン画面のビュー */
    private var rootView: View? = null
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** ログイン時の案内メッセージビュー */
    private var naviText: TextView? = null
    /** 鍵アイコン */
    private var masterKeyIcon: ImageView? = null
    /** ログインビューモデル */
    private val loginViewModel: LoginViewModel by viewModels {
        LoginViewModel.Factory(
            (requireActivity().application as PasswordMemoApplication).repository,
            (requireActivity().application as PasswordMemoApplication).loginDataManager)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_login, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginDataManager = (requireActivity().application as PasswordMemoApplication).loginDataManager

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        val versionText = rootView?.findViewById<TextView>(R.id.versionText)
        try {
            val info = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, PackageManager.GET_META_DATA)
            versionText?.text = String.format("%s %s", getString(R.string.version), info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        // パスワードログインボタンの設定
        naviText = rootView?.findViewById<View>(R.id.navigateText) as TextView
        val loginbtn = rootView?.findViewById<View>(R.id.loginButton) as Button
        loginbtn.setOnClickListener(this)
        // 生体認証ログインボタンの設定
        val biometricloginbtn = rootView?.findViewById<View>(R.id.biometricLoginButton) as ImageButton
        biometricloginbtn.setOnClickListener(this)
        // ログイン時のアニメーション設定
        masterKeyIcon = rootView?.findViewById(R.id.masterKeyIcon)
        val rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation)
        rotateAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                login()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // 案内メッセージを更新する
        lifecycleScope.launch {
            loginViewModel.naviMessage.collect { message ->
                naviText?.text = message
            }
        }

        // ログイン成功時は鍵アイコンを回転する
        lifecycleScope.launch {
            loginViewModel.keyIconRotate.collect {
                if (it) {
                    masterKeyIcon?.startAnimation(rotateAnimation)
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.loginButton -> {
                val editPassword =
                    (rootView?.findViewById<View>(R.id.editMasterPassword) as TextInputEditText).text.toString()
                context?.let { loginViewModel.passwordLogin(it, editPassword) }
            }

            R.id.biometricLoginButton -> context?.let { loginViewModel.biometricLogin(it) }
            else -> {}
        }
        (rootView?.findViewById<View>(R.id.editMasterPassword) as EditText).editableText.clear()
        loginDataManager?.updateSetting()
        checkBiometricSetting()
    }

    override fun onStart() {
        super.onStart()
        loginDataManager!!.updateSetting()
        checkBiometricSetting()
        masterKeyIcon?.clearAnimation()
        loginViewModel.resetKeyIconRotate()
        context?.let { loginViewModel.resetNaviMessage(it) }

        // 背景色を設定する
        (view?.findViewById<View>(R.id.loginView) as ConstraintLayout?)?.setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
        // 入力内容は一旦クリアする
        (view?.findViewById<View>(R.id.editMasterPassword) as EditText?)?.editableText?.clear()
        // テキストサイズを設定する
        setTextSize(loginDataManager!!.textSize)
    }

    override fun onResume() {
        super.onResume()
        loginDataManager!!.updateSetting()
        loginViewModel.firstPassword = null
        checkBiometricSetting()
        masterKeyIcon?.clearAnimation()
        loginViewModel.resetKeyIconRotate()
        context?.let { loginViewModel.resetNaviMessage(it) }
    }

    /**
     * 生体認証ログイン設定の判定処理
     * * 生体認証の有効無効設定によってボタンの活性状態を変更する
     *
     */
    private fun checkBiometricSetting() {
        if (!loginDataManager!!.biometricLoginSwitchEnable) {
            // 生体認証ログインが無効の場合は非表示
            view?.findViewById<View>(R.id.biometricLoginButton)?.visibility = View.INVISIBLE
            return
        } else if (!loginDataManager!!.isMasterPasswordCreated) {
            // 生体認証ログインが有効でマスターパスワードが作成済みなら有効化
            view?.findViewById<View>(R.id.biometricLoginButton)?.visibility = View.VISIBLE
            view?.findViewById<View>(R.id.biometricLoginButton)?.isEnabled = false
            (view?.findViewById<View>(R.id.biometricLoginButton) as ImageButton?)?.setColorFilter(
                Color.parseColor(
                    "#CCCCCC"
                )
            )
            return
        }
        val biometricManager = context?.let { BiometricManager.from(it) }
        // 生体認証判定を行う
        @Suppress("DEPRECATION")
        when (biometricManager?.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                view?.findViewById<View>(R.id.biometricLoginButton)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.biometricLoginButton)?.isEnabled = true
                view?.findViewById<ImageView>(R.id.biometricLoginButton)?.setColorFilter(
                    Color.parseColor(
                        "#007AFF"
                    )
                )
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> view?.findViewById<View>(R.id.biometricLoginButton)?.visibility =
                View.INVISIBLE

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                view?.findViewById<View>(R.id.biometricLoginButton)?.visibility = View.VISIBLE
                view?.findViewById<View>(R.id.biometricLoginButton)?.isEnabled = false
                view?.findViewById<ImageButton>(R.id.biometricLoginButton)?.setColorFilter(
                    Color.parseColor(
                        "#CCCCCC"
                    )
                )
            }

            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
            }
        }
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size 指定テキストサイズ
     */
    private fun setTextSize(size: Float) {
        view?.findViewById<TextView>(R.id.navigateText)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        view?.findViewById<EditText>(R.id.editMasterPassword)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size
        )
        view?.findViewById<Button>(R.id.loginButton)?.setTextSize(
            TypedValue.COMPLEX_UNIT_DIP,
            size - 3
        )
    }

    /**
     * ログイン処理
     * * パスワード誤り回数などをリセットして次画面に遷移する
     *
     */
    private fun login() {
        loginViewModel.incorrectPwCount = 0
        loginViewModel.firstPassword = null
        // キーボードは閉じる
        if (requireActivity().currentFocus != null) {
            val inputMethodManager =
                requireActivity().applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                requireActivity().currentFocus?.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        val intent = Intent(requireActivity(), PasswordListActivity::class.java)
        intent.putExtra("FIRST_TIME", loginViewModel.firstTime)
        requireActivity().startActivity(intent)
    }
}