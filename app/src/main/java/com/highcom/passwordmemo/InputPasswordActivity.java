package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.RandomStringUtils;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.highcom.passwordmemo.database.ListDataManager;
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter;
import com.highcom.passwordmemo.util.login.LoginDataManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InputPasswordActivity extends AppCompatActivity {

    private long id;
    private FrameLayout adContainerView;
    private AdView mAdView;
    private boolean editState;
    private LoginDataManager loginDataManager;
    private ListDataManager listDataManager;

    private int passwordKind;
    private int passwordCount;
    private boolean isLowerCaseOnly;
    private String generatePassword;
    EditText generatePasswordText;
    private long groupId;
    Spinner selectGroupSpinner;
    ArrayList<String> selectGroupNames;
    private Long selectGroupId;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_password);

        adContainerView = findViewById(R.id.adView_frame_input);
        adContainerView.post(new Runnable() {
            @Override
            public void run() {
                loadBanner();
            }
        });

        loginDataManager = LoginDataManager.getInstance(this);
        listDataManager = ListDataManager.getInstance(this);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // グループ選択スピナーの設定
        selectGroupSpinner = findViewById(R.id.selectGroup);
        selectGroupNames = new ArrayList<String>();
        for (Map<String, String> group : listDataManager.getGroupList()) {
            selectGroupNames.add(group.get("name"));
        }
        SetTextSizeAdapter selectGroupAdapter = new SetTextSizeAdapter(this, selectGroupNames, (int)loginDataManager.getTextSize());
        selectGroupSpinner.setAdapter(selectGroupAdapter);
        selectGroupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                List<Map<String, String>> groupList = listDataManager.getGroupList();
                Map<String, String> group = groupList.get(i);
                selectGroupId = Long.parseLong(Objects.requireNonNull(group.get("group_id")));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // 渡されたデータを取得する
        Intent intent = getIntent();
        id = intent.getLongExtra("ID", -1);
        groupId = intent.getLongExtra("GROUP", 1);
        editState = intent.getBooleanExtra("EDIT", false);
        ((EditText)findViewById(R.id.editTitle)).setText(intent.getStringExtra("TITLE"));
        ((EditText)findViewById(R.id.editAccount)).setText(intent.getStringExtra("ACCOUNT"));
        ((EditText)findViewById(R.id.editPassword)).setText(intent.getStringExtra("PASSWORD"));
        ((EditText)findViewById(R.id.editUrl)).setText(intent.getStringExtra("URL"));
        ((EditText)findViewById(R.id.editMemo)).setText(intent.getStringExtra("MEMO"));
        List<Map<String, String>> groupList = listDataManager.getGroupList();
        for (int i = 0; i < groupList.size(); i++) {
            long id = Long.parseLong(Objects.requireNonNull(groupList.get(i).get("group_id")));
            if (groupId == id) {
                selectGroupSpinner.setSelection(i);
                break;
            }
        }

        Button generateBtn = findViewById(R.id.generateButton);
        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePasswordDialog();
            }
        });

        // タイトルを編集にする
        if (editState) {
            setTitle(getString(R.string.edit));
        } else {
            setTitle(getString(R.string.create_new));
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void loadBanner() {
        // Create an ad request.
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.admob_unit_id_3));
        adContainerView.removeAllViews();
        adContainerView.addView(mAdView);

        AdSize adSize = getAdSize();
        mAdView.setAdSize(adSize);

        AdRequest adRequest = new AdRequest.Builder().build();

        // Start loading the ad in the background.
        mAdView.loadAd(adRequest);
    }

    private AdSize getAdSize() {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = adContainerView.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_done, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_done:
                // 入力データを登録する
                EditText editTitle = (EditText) findViewById(R.id.editTitle);
                EditText editAccount = (EditText) findViewById(R.id.editAccount);
                EditText editPassword = (EditText) findViewById(R.id.editPassword);
                EditText editUrl = (EditText) findViewById(R.id.editUrl);
                EditText editMemo = (EditText) findViewById(R.id.editMemo);

                Map<String, String> data = new HashMap<String, String>();
                data.put("id", Long.valueOf(id).toString());
                data.put("title", editTitle.getText().toString());
                data.put("account", editAccount.getText().toString());
                data.put("password", editPassword.getText().toString());
                data.put("url", editUrl.getText().toString());
                data.put("group_id", selectGroupId.toString());
                data.put("memo", editMemo.getText().toString());
                data.put("inputdate", getNowDate());

                ListDataManager.getInstance(this).setData(editState, data);
                // 詳細画面を終了
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();

        // 背景色を設定する
        findViewById(R.id.inputPasswordView).setBackgroundColor(loginDataManager.getBackgroundColor());
        // テキストサイズを設定する
        setTextSize(loginDataManager.getTextSize());
    }

    // パスワード生成ダイアログ
    private void generatePasswordDialog() {
        View generatePasswordView = getLayoutInflater().inflate(R.layout.alert_generate_password, null);
        // 文字種別ラジオボタンの初期値を設定
        RadioGroup passwordRadio = generatePasswordView.findViewById(R.id.passwordKindMenu);
        passwordRadio.check(R.id.radioLettersNumbers);
        passwordKind = passwordRadio.getCheckedRadioButtonId();
        passwordRadio.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                passwordKind = checkedId;
                generatePasswordString();
            }
        });
        // 小文字のみチェクボックスを設定
        CheckBox lowerCaseOnlyCheckBox = generatePasswordView.findViewById(R.id.lowerCaseOnly);
        lowerCaseOnlyCheckBox.setChecked(false);
        isLowerCaseOnly = false;
        lowerCaseOnlyCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLowerCaseOnly = lowerCaseOnlyCheckBox.isChecked();
                if (isLowerCaseOnly) {
                    generatePasswordText.setText(generatePassword.toLowerCase());
                } else {
                    generatePasswordText.setText(generatePassword);
                }
            }
        });

        // 文字数ピッカーの初期値を設定
        NumberPicker passwordPicker = generatePasswordView.findViewById(R.id.passwordNumberPicker);
        passwordPicker.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        passwordPicker.setMaxValue(32);
        passwordPicker.setMinValue(4);
        passwordPicker.setValue(8);
        passwordCount = passwordPicker.getValue();
        passwordPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                passwordCount = newVal;
                generatePasswordString();
            }
        });
        // ボタンのカラーフィルターとイベントを設定
        ImageButton generateButton = generatePasswordView.findViewById(R.id.generateButton);
        generateButton.setColorFilter(Color.parseColor("#007AFF"));
        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generatePasswordString();
            }
        });

        // ダイアログの生成
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.generate_password_title)
                .setView(generatePasswordView)
                .setPositiveButton(R.string.apply, null)
                .setNegativeButton(R.string.discard, null)
                .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isLowerCaseOnly) {
                    ((EditText)findViewById(R.id.editPassword)).setText(generatePassword.toLowerCase());
                } else {
                    ((EditText)findViewById(R.id.editPassword)).setText(generatePassword);
                }
                alertDialog.dismiss();
            }
        });
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        generatePasswordText = generatePasswordView.findViewById(R.id.generatePasswordText);
        generatePasswordString();
    }

    // パスワード文字列生成
    private void generatePasswordString()
    {
        if (passwordKind == R.id.radioNumbers) {
            generatePassword = RandomStringUtils.randomNumeric(passwordCount);
        } else if (passwordKind == R.id.radioLettersNumbers){
            generatePassword = RandomStringUtils.randomAlphanumeric(passwordCount);
        } else {
            generatePassword = RandomStringUtils.randomGraph(passwordCount);
        }

        if (isLowerCaseOnly) {
            generatePasswordText.setText(generatePassword.toLowerCase());
        } else {
            generatePasswordText.setText(generatePassword);
        }
    }

    // 現在の日付取得処理
    public String getNowDate(){
        Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.format(date);
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) mAdView.destroy();
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager.isDisplayBackgroundSwitchEnable() && PasswordMemoLifecycle.getIsBackground()) {
            finishAffinity();
        }
        super.onDestroy();
    }

    private void setTextSize(float size) {
        ((TextView)findViewById(R.id.titleView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editTitle)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.accountView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editAccount)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.passwordView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editPassword)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.generateButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.urlView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editUrl)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.groupView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        SetTextSizeAdapter selectGroupAdapter = new SetTextSizeAdapter(this, selectGroupNames, (int)loginDataManager.getTextSize());
        selectGroupSpinner.setAdapter(selectGroupAdapter);
        List<Map<String, String>> groupList = listDataManager.getGroupList();
        for (int i = 0; i < groupList.size(); i++) {
            long id = Long.parseLong(Objects.requireNonNull(groupList.get(i).get("group_id")));
            if (groupId == id) {
                selectGroupSpinner.setSelection(i);
                break;
            }
        }
        ((EditText)findViewById(R.id.editMemo)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }
}
