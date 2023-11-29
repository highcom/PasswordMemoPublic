package com.highcom.passwordmemo.util.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

import com.highcom.passwordmemo.database.ListDataManager;
import com.highcom.passwordmemo.PasswordListActivity;
import com.highcom.passwordmemo.R;

import java.util.concurrent.Executor;

public class LoginService {
    LoginDataManager loginDataManager;
    private int incorrectPwCount = 0;
    Boolean firstTime = false;
    String firstPassword = null;
    private Animation rotateAnimation;
    TextView navigateText;
    ImageView masterKeyIcon;

    public LoginService(LoginDataManager manager) {
        loginDataManager = manager;
    }

    public void clearFirstEditPassword() {
        firstPassword = null;
    }

    public String passwordLogin(final Activity activity, String editPassword) {
        navigateText = activity.findViewById(R.id.navigateText);
        masterKeyIcon = activity.findViewById(R.id.masterKeyIcon);
        String message = null;
        String masterPassword = loginDataManager.getMasterPassword();
        rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation);
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                login(activity, firstTime);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        if (editPassword.equals("")) {
            message = activity.getString(R.string.err_empty);
        } else if (!loginDataManager.isMasterPasswordCreated()) {
            if (firstPassword == null) {
                firstPassword = editPassword;
                message = activity.getString(R.string.err_input_same);
            } else if (editPassword.equals(firstPassword)) {
                // マスターパスワードが作成されていない場合は新規作成
                loginDataManager.setMasterPassword(editPassword);
                // ログイン中の表示に切り替える
                firstTime = true;
                navigateText.setText(activity.getString(R.string.login_success));
                masterKeyIcon.startAnimation(rotateAnimation);
            } else {
                // 一度目の入力と異なることを伝える
                Toast ts = Toast.makeText(activity, activity.getString(R.string.err_input_different), Toast.LENGTH_SHORT);
                ts.show();
                firstPassword = null;
                message = activity.getString(R.string.new_password);
            }
        } else if (editPassword.equals(masterPassword)) {
            // ログイン中の表示に切り替える
            firstTime = false;
            navigateText.setText(activity.getString(R.string.login_success));
            masterKeyIcon.startAnimation(rotateAnimation);
        } else if (!loginDataManager.isDeleteSwitchEnable()) {
            // データ削除機能が無効の場合にはエラー表示を行うだけ
            message = activity.getString(R.string.err_incorrect);
        } else {
            // データ削除機能が有効の場合には5回連続で間違えるとデータを削除する
            incorrectPwCount += 1;
            if (incorrectPwCount >= 5) {
                incorrectPwCount = 0;
                loginDataManager.clearAllData();
                ListDataManager manager = ListDataManager.getInstance(activity);
                manager.deleteAllData();
                manager.closeData();
                // すべてのデータを削除したことを表示
                Toast ts = Toast.makeText(activity, activity.getString(R.string.err_delete_all), Toast.LENGTH_LONG);
                ts.show();
                message = activity.getString(R.string.new_password);
            } else {
                message = activity.getString(R.string.err_incorrect_num_front) + incorrectPwCount + activity.getString(R.string.err_incorrect_num_back);
            }
        }
        return message;
    }

    public void clearAnimation() {
        if (masterKeyIcon != null) masterKeyIcon.clearAnimation();
    }

    private Handler handler = new Handler();

    private Executor executor = new Executor() {
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };

    public void biometricLogin(final Activity activity) {
        navigateText = activity.findViewById(R.id.navigateText);
        masterKeyIcon = activity.findViewById(R.id.masterKeyIcon);
        rotateAnimation = AnimationUtils.loadAnimation(activity, R.anim.rotate_animation);
        rotateAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                login(activity, false);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle(activity.getString(R.string.login_biometrics))
                        .setSubtitle(activity.getString(R.string.login_biometrics_message))
                        .setNegativeButtonText(activity.getString(R.string.cancel))
                        .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt((FragmentActivity) activity,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(activity.getApplicationContext(),
                        activity.getString(R.string.err_authentication_message) + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // ログイン中の表示に切り替える
                firstTime = false;
                navigateText.setText(activity.getString(R.string.login_success));
                masterKeyIcon.startAnimation(rotateAnimation);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(activity.getApplicationContext(), activity.getString(R.string.err_authentication_failure),
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });

        // Displays the "log in" prompt.
        biometricPrompt.authenticate(promptInfo);
    }

    private void login(final Activity activity, final boolean first_time) {
        incorrectPwCount = 0;
        firstPassword = null;
        // キーボードは閉じる
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) activity.getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        Intent intent = new Intent(activity, PasswordListActivity.class);
        intent.putExtra("FIRST_TIME", first_time);
        activity.startActivity(intent);
    }
}
