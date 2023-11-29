package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.highcom.passwordmemo.util.login.LoginDataManager;

public class LicenseActivity extends AppCompatActivity {
    private LoginDataManager loginDataManager;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        setTitle(getString(R.string.license));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginDataManager = LoginDataManager.getInstance(this);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();
        ((LinearLayout)findViewById(R.id.licenseView)).setBackgroundColor(LoginDataManager.getInstance(this).getBackgroundColor());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager.isDisplayBackgroundSwitchEnable() && PasswordMemoLifecycle.getIsBackground()) {
            finishAffinity();
        }
        super.onDestroy();
    }
}