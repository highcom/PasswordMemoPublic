package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.highcom.passwordmemo.util.login.LoginDataManager;
import com.highcom.passwordmemo.util.login.LoginService;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private LoginDataManager loginDataManager;
    LoginService loginService;
    TextView naviText;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_login);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        loginDataManager = LoginDataManager.getInstance(this);
        loginService = new LoginService(loginDataManager);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        TextView versionText = findViewById(R.id.versionText);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            versionText.setText(String.format("%s %s", getString(R.string.version), info.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        naviText = (TextView) findViewById(R.id.navigateText);

        Button loginbtn = (Button) findViewById(R.id.loginButton);
        loginbtn.setOnClickListener(this);

        ImageButton biometricloginbtn = (ImageButton) findViewById(R.id.biometricLoginButton);
        biometricloginbtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.loginButton:
                    String editPassword = ((TextInputEditText)findViewById(R.id.editMasterPassword)).getText().toString();
                    String naviMessage = loginService.passwordLogin(this, editPassword);
                    if (naviMessage != null) naviText.setText(naviMessage);
                    break;
                case R.id.biometricLoginButton:
                    loginService.biometricLogin(this);
                    break;
                default:
                    break;
            }
            ((EditText)findViewById(R.id.editMasterPassword)).getEditableText().clear();
            loginDataManager.updateSetting();
            checkBiometricSetting();
        }
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();
        loginDataManager.updateSetting();
        checkBiometricSetting();
        loginService.clearAnimation();
        if (loginDataManager.isMasterPasswordCreated()) {
            naviText.setText(R.string.input_password);
        } else {
            naviText.setText(R.string.new_password);
        }

        // 背景色を設定する
        ((ConstraintLayout)findViewById(R.id.loginView)).setBackgroundColor(loginDataManager.getBackgroundColor());
        // 入力内容は一旦クリアする
        ((EditText)findViewById(R.id.editMasterPassword)).getEditableText().clear();
        // テキストサイズを設定する
        setTextSize(loginDataManager.getTextSize());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loginDataManager.updateSetting();
        loginService.clearFirstEditPassword();
        checkBiometricSetting();
        loginService.clearAnimation();
        if (loginDataManager.isMasterPasswordCreated()) {
            naviText.setText(R.string.input_password);
        } else {
            naviText.setText(R.string.new_password);
        }
    }

    private void checkBiometricSetting() {
        if (!loginDataManager.isBiometricLoginSwitchEnable()) {
            findViewById(R.id.biometricLoginButton).setVisibility(View.INVISIBLE);
            return;
        } else if (!loginDataManager.isMasterPasswordCreated()) {
            findViewById(R.id.biometricLoginButton).setVisibility(View.VISIBLE);
            findViewById(R.id.biometricLoginButton).setEnabled(false);
            ((ImageButton)findViewById(R.id.biometricLoginButton)).setColorFilter(Color.parseColor("#CCCCCC"));
            return;
        }

        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                findViewById(R.id.biometricLoginButton).setVisibility(View.VISIBLE);
                findViewById(R.id.biometricLoginButton).setEnabled(true);
                ((ImageButton)findViewById(R.id.biometricLoginButton)).setColorFilter(Color.parseColor("#007AFF"));
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                findViewById(R.id.biometricLoginButton).setVisibility(View.INVISIBLE);
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                findViewById(R.id.biometricLoginButton).setVisibility(View.VISIBLE);
                findViewById(R.id.biometricLoginButton).setEnabled(false);
                ((ImageButton)findViewById(R.id.biometricLoginButton)).setColorFilter(Color.parseColor("#CCCCCC"));
                break;
        }
    }

    private void setTextSize(float size) {
        ((TextView)findViewById(R.id.navigateText)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((EditText)findViewById(R.id.editMasterPassword)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.loginButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
    }
}
