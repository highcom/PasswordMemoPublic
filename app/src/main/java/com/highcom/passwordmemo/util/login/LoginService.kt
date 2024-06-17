package com.highcom.passwordmemo.util.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import com.highcom.passwordmemo.PasswordListActivity
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import java.util.concurrent.Executor

class LoginService(private var loginDataManager: LoginDataManager?, private val passwordListViewModel: PasswordListViewModel) {
    private var incorrectPwCount = 0
    var firstTime = false
    var firstPassword: String? = null
    private var rotateAnimation: Animation? = null
    var navigateText: TextView? = null
    var masterKeyIcon: ImageView? = null
    fun clearFirstEditPassword() {
        firstPassword = null
    }

    fun passwordLogin(activity: Activity, editPassword: String): String? {
        navigateText = activity.findViewById(R.id.navigateText)
        masterKeyIcon = activity.findViewById(R.id.masterKeyIcon)
        var message: String? = null
        val masterPassword = loginDataManager!!.masterPassword
        rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation)
        rotateAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                login(activity, firstTime)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        if (editPassword == "") {
            message = activity.getString(R.string.err_empty)
        } else if (!loginDataManager!!.isMasterPasswordCreated) {
            if (firstPassword == null) {
                firstPassword = editPassword
                message = activity.getString(R.string.err_input_same)
            } else if (editPassword == firstPassword) {
                // マスターパスワードが作成されていない場合は新規作成
                loginDataManager!!.setMasterPassword(editPassword)
                // ログイン中の表示に切り替える
                firstTime = true
                navigateText?.setText(activity.getString(R.string.login_success))
                masterKeyIcon?.startAnimation(rotateAnimation)
            } else {
                // 一度目の入力と異なることを伝える
                val ts = Toast.makeText(
                    activity,
                    activity.getString(R.string.err_input_different),
                    Toast.LENGTH_SHORT
                )
                ts.show()
                firstPassword = null
                message = activity.getString(R.string.new_password)
            }
        } else if (editPassword == masterPassword) {
            // ログイン中の表示に切り替える
            firstTime = false
            navigateText?.setText(activity.getString(R.string.login_success))
            masterKeyIcon?.startAnimation(rotateAnimation)
        } else if (!loginDataManager!!.deleteSwitchEnable) {
            // データ削除機能が無効の場合にはエラー表示を行うだけ
            message = activity.getString(R.string.err_incorrect)
        } else {
            // データ削除機能が有効の場合には5回連続で間違えるとデータを削除する
            incorrectPwCount += 1
            if (incorrectPwCount >= 5) {
                incorrectPwCount = 0
//                loginDataManager!!.clearAllData()
//                val manager: ListDataManager? = ListDataManager.Companion.getInstance(activity)
//                manager?.deleteAllData()
//                manager?.closeData()
                // TODO:グループの全削除と初期グループの登録を実施する必要がある
                passwordListViewModel.deleteAll()
                // すべてのデータを削除したことを表示
                val ts = Toast.makeText(
                    activity,
                    activity.getString(R.string.err_delete_all),
                    Toast.LENGTH_LONG
                )
                ts.show()
                message = activity.getString(R.string.new_password)
            } else {
                message =
                    activity.getString(R.string.err_incorrect_num_front) + incorrectPwCount + activity.getString(
                        R.string.err_incorrect_num_back
                    )
            }
        }
        return message
    }

    fun clearAnimation() {
        if (masterKeyIcon != null) masterKeyIcon!!.clearAnimation()
    }

    private val handler = Handler()
    private val executor = Executor { command -> handler.post(command) }
    fun biometricLogin(activity: Activity) {
        navigateText = activity.findViewById(R.id.navigateText)
        masterKeyIcon = activity.findViewById(R.id.masterKeyIcon)
        rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation)
        rotateAnimation?.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                login(activity, false)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })
        val promptInfo = PromptInfo.Builder()
            .setTitle(activity.getString(R.string.login_biometrics))
            .setSubtitle(activity.getString(R.string.login_biometrics_message))
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .build()
        val biometricPrompt = BiometricPrompt(
            (activity as FragmentActivity),
            executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        activity.getApplicationContext(),
                        activity.getString(R.string.err_authentication_message) + errString,
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // ログイン中の表示に切り替える
                    firstTime = false
                    navigateText?.setText(activity.getString(R.string.login_success))
                    masterKeyIcon?.startAnimation(rotateAnimation)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        activity.getApplicationContext(),
                        activity.getString(R.string.err_authentication_failure),
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            })

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo)
    }

    private fun login(activity: Activity, first_time: Boolean) {
        incorrectPwCount = 0
        firstPassword = null
        // キーボードは閉じる
        if (activity.currentFocus != null) {
            val inputMethodManager =
                activity.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(
                activity.currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
        val intent = Intent(activity, PasswordListActivity::class.java)
        intent.putExtra("FIRST_TIME", first_time)
        activity.startActivity(intent)
    }
}