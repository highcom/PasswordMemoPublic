package com.highcom.passwordmemo.ui.fragment

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.viewmodel.LoginViewModel
import com.highcom.passwordmemo.domain.DarkModeUtil
import com.highcom.passwordmemo.domain.login.LoginDataManager
import kotlinx.coroutines.launch
import com.highcom.passwordmemo.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * ログイン画面フラグメント
 *
 */
@AndroidEntryPoint
class LoginFragment : Fragment() {
    /** ログイン画面のbinding */
    private lateinit var binding: FragmentLoginBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** ログインビューモデル */
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)
        binding.loginViewModel = loginViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.app_name)
        // ログイン画面では全てのメニューを表示しない
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) activity.allMenuDisabled()

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        try {
            val info = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, PackageManager.GET_META_DATA)
            binding.versionText.text = String.format("%s %s", getString(R.string.version), info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        // ログイン時のアニメーション設定
        val rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation)
        rotateAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                login()
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // ナビゲーションメッセージ更新時にボタンの有効無効を更新
        lifecycleScope.launch {
            loginViewModel.naviMessage.collect {
                checkBiometricSetting()
            }
        }

        // ログイン成功時は鍵アイコンを回転する
        lifecycleScope.launch {
            loginViewModel.keyIconRotate.collect {
                if (it) {
                    binding.masterKeyIcon.startAnimation(rotateAnimation)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        loginDataManager.updateSetting()
        checkBiometricSetting()
        binding.masterKeyIcon.clearAnimation()
        loginViewModel.resetKeyIconRotate()
        loginViewModel.resetNaviMessage(requireContext())

        // 戻るボタンを無効化
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
        // 背景色を設定する（ダークモード時はテーマの色を優先）
        if (!DarkModeUtil.isDarkModeEnabled(requireContext(), loginDataManager.darkMode)) {
            binding.loginFragmentView.setBackgroundColor(loginDataManager.backgroundColor)
        }
        // 入力内容は一旦クリアする
        binding.editMasterPassword.editableText?.clear()
        // テキストサイズを設定する
        setTextSize(loginDataManager.textSize)
    }

    override fun onResume() {
        super.onResume()
        loginDataManager.updateSetting()
        loginViewModel.firstPassword = null
        checkBiometricSetting()
        binding.masterKeyIcon.clearAnimation()
        loginViewModel.resetKeyIconRotate()
        loginViewModel.resetNaviMessage(requireContext())
    }

    /**
     * 生体認証ログイン設定の判定処理
     * * 生体認証の有効無効設定によってボタンの活性状態を変更する
     *
     */
    private fun checkBiometricSetting() {
        if (!loginDataManager.biometricLoginSwitchEnable) {
            // 生体認証ログインが無効の場合は非表示
            binding.biometricLoginButton.visibility = View.INVISIBLE
            return
        } else if (!loginDataManager.isMasterPasswordCreated) {
            // 生体認証ログインが有効でマスターパスワードが作成済みなら有効化
            binding.biometricLoginButton.visibility = View.VISIBLE
            binding.biometricLoginButton.isEnabled = false
            binding.biometricLoginButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
            return
        }
        val biometricManager = context?.let { BiometricManager.from(it) }
        // 生体認証判定を行う
        @Suppress("DEPRECATION")
        when (biometricManager?.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.biometricLoginButton.visibility = View.VISIBLE
                binding.biometricLoginButton.isEnabled = true
                binding.biometricLoginButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.blue))
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> binding.biometricLoginButton.visibility = View.INVISIBLE

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                binding.biometricLoginButton.visibility = View.VISIBLE
                binding.biometricLoginButton.isEnabled = false
                binding.biometricLoginButton.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray))
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
        binding.navigateText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.editMasterPassword.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
        binding.loginButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size - 3)
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
        findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToPasswordListFragment(firstTime = loginViewModel.firstTime))
    }
}