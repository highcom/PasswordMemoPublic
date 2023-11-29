package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

import com.google.android.material.textfield.TextInputEditText;
import com.highcom.passwordmemo.ui.list.SetTextSizeAdapter;
import com.highcom.passwordmemo.util.BackgroundColorUtil;
import com.highcom.passwordmemo.util.TextSizeUtil;
import com.highcom.passwordmemo.util.file.BackupDbFile;
import com.highcom.passwordmemo.util.file.InputExternalFile;
import com.highcom.passwordmemo.util.file.OutputExternalFile;
import com.highcom.passwordmemo.util.file.RestoreDbFile;
import com.highcom.passwordmemo.util.file.SelectInputOutputFileDialog;
import com.highcom.passwordmemo.util.login.LoginDataManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class SettingActivity extends AppCompatActivity implements BackgroundColorUtil.BackgroundColorListener, TextSizeUtil.TextSizeListener, SelectInputOutputFileDialog.InputOutputFileDialogListener, RestoreDbFile.RestoreDbFileListener, InputExternalFile.InputExternalFileListener {
    public static final int NEED_UPDATE = 1;

    private static final int RESTORE_DB = 1001;
    private static final int BACKUP_DB = 1002;
    private static final int INPUT_CSV = 1003;
    private static final int OUTPUT_CSV = 1004;
    private LoginDataManager loginDataManager;
    private Handler handler = new Handler();

    Spinner copyClipboardSpinner;
    ArrayList<String> copyClipboardNames;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        setTitle(getString(R.string.setting));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginDataManager = LoginDataManager.getInstance(this);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // 背景色を設定する
        ((ScrollView)findViewById(R.id.settingView)).setBackgroundColor(loginDataManager.getBackgroundColor());

        // データ削除スイッチ処理
        Switch deleteSwitch = (Switch) findViewById(R.id.deleteSwitch);
        deleteSwitch.setChecked(loginDataManager.isDeleteSwitchEnable());
        deleteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                loginDataManager.setDeleteSwitchEnable(b);
            }
        });

        // 生体認証ログインスイッチ処理
        Switch biometricLoginSwitch = (Switch) findViewById(R.id.biometricLoginSwitch);
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {
            biometricLoginSwitch.setChecked(false);
            biometricLoginSwitch.setEnabled(false);
        } else {
            biometricLoginSwitch.setChecked(loginDataManager.isBiometricLoginSwitchEnable());
            biometricLoginSwitch.setEnabled(true);
        }
        biometricLoginSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                loginDataManager.setBiometricLoginSwitchEnable(b);
            }
        });

        // バックグラウンド時の非表示設定
        Switch displayBackgroundSwitch = (Switch) findViewById(R.id.displayBackgroundSwitch);
        displayBackgroundSwitch.setChecked(loginDataManager.isDisplayBackgroundSwitchEnable());
        displayBackgroundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                loginDataManager.setDisplayBackgroundSwitchEnable(b);
                restartPasswordMemoActivity();
            }
        });

        // メモ表示スイッチ処理
        Switch memoVisibleSwitch = (Switch) findViewById(R.id.memoVisibleSwitch);
        memoVisibleSwitch.setChecked(loginDataManager.isMemoVisibleSwitchEnable());
        memoVisibleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                loginDataManager.setMemoVisibleSwitchEnable(b);
            }
        });

        // テキストサイズスピナー処理
        Spinner textSizeSpinner = (Spinner) findViewById(R.id.textSizeSpinner);
        TextSizeUtil textSizeUtil = new TextSizeUtil(getApplicationContext(), this);
        textSizeUtil.createTextSizeSpinner(textSizeSpinner);
        textSizeSpinner.setSelection(textSizeUtil.getSpecifiedValuePosition(loginDataManager.getTextSize()));

        // パスワードコピー方法スピナー処理
        copyClipboardSpinner = findViewById(R.id.copyClipboardSpinner);
        copyClipboardNames = new ArrayList<String>();
        copyClipboardNames.add(getString(R.string.copy_with_longpress));
        copyClipboardNames.add(getString(R.string.copy_with_tap));
        SetTextSizeAdapter copyClipboardAdapter = new SetTextSizeAdapter(this, copyClipboardNames, (int)loginDataManager.getTextSize());
        copyClipboardSpinner.setAdapter(copyClipboardAdapter);
        copyClipboardSpinner.setSelection(loginDataManager.getCopyClipboard());
        copyClipboardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                loginDataManager.setCopyClipboard(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // DBバックアップ復元ボタン処理
        Button dbBackupBtn = (Button) findViewById(R.id.dbBackupButton);
        dbBackupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                confirmSelectOperation(SelectInputOutputFileDialog.Operation.DB_RESTORE_BACKUP);
            }
        });

        // CSV出力ボタン処理
        Button csvOutputBtn = (Button) findViewById(R.id.csvOutputButton);
        csvOutputBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                confirmSelectOperation(SelectInputOutputFileDialog.Operation.CSV_INPUT_OUTPUT);
            }
        });

        // 背景色ボタン処理
        Button colorSelectBtn = (Button) findViewById(R.id.colorSelectButton);
        colorSelectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                colorSelectDialog();
            }
        });

        // パスワード設定ボタン処理
        Button masterPasswordSetBtn = (Button) findViewById(R.id.masterPasswordSetButton);
        masterPasswordSetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                editMasterPassword();
            }
        });

        // 操作説明ボタン処理
        Button operationInstructionBtn = (Button) findViewById(R.id.operationInstructionButton);
        operationInstructionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                operationInstructionDialog();
            }
        });

        // このアプリを評価ボタン押下処理
        Button rateBtn = (Button) findViewById(R.id.RateButton);
        rateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.highcom.passwordmemo");
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
            }
        });

        // ライセンスボタン処理
        Button licenseBtn = (Button) findViewById(R.id.licenseButton);
        licenseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(SettingActivity.this, LicenseActivity.class);
                startActivity(intent);
            }
        });

        // プライバシーポリシーボタン処理
        Button PrivacyPolicyBtn = (Button) findViewById(R.id.PrivacyPolicyButton);
        PrivacyPolicyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Uri uri = Uri.parse("https://high-commu.amebaownd.com/pages/2891722/page_201905200001");
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                startActivity(intent);
            }
        });

        // テキストサイズを設定する
        setTextSize(loginDataManager.getTextSize());
    }

    private void restartPasswordMemoActivity() {
        Toast ts = Toast.makeText(this, getString(R.string.restart_message), Toast.LENGTH_SHORT);
        ts.show();
        Runnable restartRunnable = new Runnable() {
            @Override
            public void run() {
                executeRestart();
            }
        };
        handler.postDelayed(restartRunnable, 500);
    }

    private void executeRestart() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // 起動しているActivityをすべて削除し、新しいタスクでMainActivityを起動する
        startActivity(intent);
    }

    // パスワード変更完了時の表示
    private void passwordChangeComplete()
    {
        Toast ts = Toast.makeText(this, getString(R.string.password_change_message), Toast.LENGTH_SHORT);
        ts.show();
    }

    private void confirmSelectOperation(SelectInputOutputFileDialog.Operation operation) {
        SelectInputOutputFileDialog selectInputOutputFileDialog = new SelectInputOutputFileDialog(this, operation, this);
        selectInputOutputFileDialog.createOpenFileDialog().show();
    }

    @Override
    public void onSelectOperationClicked(String path) {
        if (path.equals(getString(R.string.restore_db))) {
            confirmRestoreSelectFile();
        } else if (path.equals(getString(R.string.backup_db))) {
            confirmBackupSelectFile();
        } else if (path.equals(getString(R.string.input_csv))) {
            confirmInputSelectFile();
        } else if (path.equals(getString(R.string.output_csv))) {
            confirmOutputSelectFile();
        }
    }

    private void confirmRestoreSelectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, RESTORE_DB);
    }

    private void confirmBackupSelectFile() {
        String fileName = "PasswordMemoDB_" + getNowDateString() + ".db";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/db");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, BACKUP_DB);
    }

    private void confirmInputSelectFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");

        startActivityForResult(intent, INPUT_CSV);
    }

    private void confirmOutputSelectFile() {
        String fileName = "PasswordListFile_" + getNowDateString() + ".csv";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);

        startActivityForResult(intent, OUTPUT_CSV);
    }

    private String getNowDateString(){
        Date date = new Date();
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        return sdf.format(date);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData );

        if (resultCode == Activity.RESULT_OK && resultData.getData() != null) {
            Uri uri = resultData.getData();
            switch (requestCode) {
                case RESTORE_DB:
                    RestoreDbFile restoreDbFile = new RestoreDbFile(this, this);
                    restoreDbFile.restoreSelectFolder(uri);
                    break;
                case BACKUP_DB:
                    BackupDbFile backupDbFile = new BackupDbFile(this);
                    backupDbFile.backupSelectFolder(uri);
                    break;
                case INPUT_CSV:
                    InputExternalFile inputExternalFile = new InputExternalFile(this, this);
                    inputExternalFile.inputSelectFolder(uri);
                    break;
                case OUTPUT_CSV:
                    OutputExternalFile outputExternalFile = new OutputExternalFile(this);
                    outputExternalFile.outputSelectFolder(uri);
                    break;
            }
        }
    }

    private void colorSelectDialog() {
        BackgroundColorUtil backgroundColorUtil = new BackgroundColorUtil(getApplicationContext(), this);
        backgroundColorUtil.createBackgroundColorDialog(this);
    }

    private void editMasterPassword() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.change_master_password)
                .setView(getLayoutInflater().inflate(R.layout.alert_edit_master_password, null))
                .setPositiveButton(R.string.execute, null)
                .setNegativeButton(R.string.discard, null)
                .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String orgPassword = loginDataManager.getMasterPassword();
                String newPassword = ((TextInputEditText)alertDialog.findViewById(R.id.editNewMasterPassword)).getText().toString();
                String newPassword2 = ((TextInputEditText)alertDialog.findViewById(R.id.editNewMasterPassword2)).getText().toString();

                if (!newPassword.equals(newPassword2)) {
                    // 入力内容が異なっていたらエラー
                    ((TextView) alertDialog.findViewById(R.id.inputNavigateText)).setText(R.string.input_different_message);
                    return;
                } else if (newPassword.equals("")) {
                    // 入力内容が空ならエラー
                    ((TextView) alertDialog.findViewById(R.id.inputNavigateText)).setText(R.string.nothing_entered_message);
                    return;
                } else if (newPassword.equals(orgPassword)) {
                    // マスターパスワードと同じならエラー
                    ((TextView) alertDialog.findViewById(R.id.inputNavigateText)).setText(R.string.password_same_message);
                    return;
                } else {
                    ((TextView) alertDialog.findViewById(R.id.inputNavigateText)).setText(" ");
                    loginDataManager.setMasterPassword(newPassword);
                    passwordChangeComplete();
                    alertDialog.dismiss();
                }
            }
        });
    }

    private void operationInstructionDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.operation_instruction)
                .setView(getLayoutInflater().inflate(R.layout.alert_operating_instructions, null))
                .setPositiveButton(R.string.close, null)
                .create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    @SuppressLint("ResourceType")
    @Override
    public void onSelectColorClicked(int color) {
        ((ScrollView)findViewById(R.id.settingView)).setBackgroundColor(color);
        loginDataManager.setBackgroundColor(color);
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
    public void onTextSizeSelected(float size) {
        setTextSize(size);
        loginDataManager.setTextSize(size);
    }

    private void setTextSize(float size) {
        // レイアウトが崩れるので詳細説明のテキストはサイズ変更しない
        ((Switch)findViewById(R.id.deleteSwitch)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Switch)findViewById(R.id.biometricLoginSwitch)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Switch)findViewById(R.id.displayBackgroundSwitch)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Switch)findViewById(R.id.memoVisibleSwitch)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.textSizeView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        // テキストサイズ設定のSpinnerは設定不要
        ((TextView)findViewById(R.id.copyClipboardView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        SetTextSizeAdapter copyClipboardAdapter = new SetTextSizeAdapter(this, copyClipboardNames, (int)size);
        copyClipboardSpinner.setAdapter(copyClipboardAdapter);
        copyClipboardSpinner.setSelection(loginDataManager.getCopyClipboard());
        ((TextView)findViewById(R.id.textDbBackupView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.dbBackupButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textCsvOutputView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.csvOutputButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textColorSelectView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.colorSelectButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textMasterPasswordView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.masterPasswordSetButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textOperationInstructionView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.operationInstructionButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textRateView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.RateButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textLicenseView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.licenseButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((TextView)findViewById(R.id.textPrivacyPolicyView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((Button)findViewById(R.id.PrivacyPolicyButton)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
    }

    @Override
    public void restoreComplete() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // 起動しているActivityをすべて削除し、新しいタスクでLoginActivityを起動する
        startActivity(intent);
    }

    @Override
    public void importComplete() {
        setResult(NEED_UPDATE);
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
