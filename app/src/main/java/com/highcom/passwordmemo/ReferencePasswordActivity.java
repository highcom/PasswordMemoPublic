package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.material.snackbar.Snackbar;
import com.highcom.passwordmemo.database.ListDataManager;
import com.highcom.passwordmemo.util.login.LoginDataManager;

import java.util.List;
import java.util.Map;

public class ReferencePasswordActivity extends AppCompatActivity {
    private static final int OPERATION_LONGPRESS = 0;
    private static final int OPERATION_TAP = 1;

    private LoginDataManager loginDataManager;
    private ListDataManager listDataManager;
    private long id;
    private long groupId;
    private FrameLayout adContainerView;
    private AdView mAdView;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reference_password);

        adContainerView = findViewById(R.id.adView_frame_reference);
        adContainerView.post(new Runnable() {
            @Override
            public void run() {
                loadBanner();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginDataManager = LoginDataManager.getInstance(this);
        listDataManager = ListDataManager.getInstance(this);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        // 渡されたデータを取得する
        Intent intent = getIntent();
        id = intent.getLongExtra("ID", -1);
        groupId = intent.getLongExtra("GROUP", 1);
        setTitle(intent.getStringExtra("TITLE"));
        ((EditText)findViewById(R.id.editRefAccount)).setText(intent.getStringExtra("ACCOUNT"));
        ((EditText)findViewById(R.id.editRefPassword)).setText(intent.getStringExtra("PASSWORD"));
        ((EditText)findViewById(R.id.editRefUrl)).setText(intent.getStringExtra("URL"));
        ((EditText)findViewById(R.id.editRefMemo)).setText(intent.getStringExtra("MEMO"));
        List<Map<String, String>> groupList = listDataManager.getGroupList();
        for (Map<String, String> group : groupList) {
            if (groupId == Long.valueOf(group.get("group_id"))) {
                ((EditText)findViewById(R.id.editRefGroup)).setText(group.get("name"));
                break;
            }
        }

        // アカウントIDをクリックor長押し時の処理
        EditText accountText = (EditText) findViewById(R.id.editRefAccount);
        accountText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginDataManager.getCopyClipboard() == OPERATION_TAP) {
                    CopyClipBoard(v, ((EditText) findViewById(R.id.editRefAccount)).getText().toString());
                }
            }
        });
        accountText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (loginDataManager.getCopyClipboard() == OPERATION_LONGPRESS) {
                    CopyClipBoard(arg0, ((EditText) findViewById(R.id.editRefAccount)).getText().toString());
                }
                return true;
            }
        });

        EditText passwordText = (EditText) findViewById(R.id.editRefPassword);
        // パスワードの初期表示設定
        if (loginDataManager.isPasswordVisibleSwitchEnable()) passwordText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        // パスワードをクリックor長押し時の処理
        passwordText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loginDataManager.getCopyClipboard() == OPERATION_TAP) {
                    CopyClipBoard(v, ((EditText) findViewById(R.id.editRefPassword)).getText().toString());
                }
            }
        });
        passwordText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View arg0) {
                if (loginDataManager.getCopyClipboard() == OPERATION_LONGPRESS) {
                    CopyClipBoard(arg0, ((EditText) findViewById(R.id.editRefPassword)).getText().toString());
                }
                return true;
            }
        });

        // URLをクリック時の処理
        final EditText urlText = (EditText) findViewById(R.id.editRefUrl);
        urlText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 何も入力されていなかったら何もしない
                if (urlText.getText().toString().equals("")) return;
                Uri uri = Uri.parse(urlText.getText().toString());
                Intent intent = new Intent(Intent.ACTION_VIEW,uri);
                Intent chooser = Intent.createChooser(intent, "選択");
                startActivity(chooser);
            }
        });
    }

    private void loadBanner() {
        // Create an ad request.
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.admob_unit_id_2));
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
        getMenuInflater().inflate(R.menu.menu_reference, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case android.R.id.home:
            default:
                finish();
                break;
            case R.id.action_copy:
                // 入力画面を生成
                intent = new Intent(ReferencePasswordActivity.this, InputPasswordActivity.class);
                // 選択アイテムを複製モードで設定
                intent.putExtra("ID", listDataManager.getNewId());
                intent.putExtra("EDIT", false);
                intent.putExtra("TITLE", getTitle().toString() + " " + getString(R.string.copy_title));
                intent.putExtra("ACCOUNT", ((EditText)findViewById(R.id.editRefAccount)).getText().toString());
                intent.putExtra("PASSWORD", ((EditText)findViewById(R.id.editRefPassword)).getText().toString());
                intent.putExtra("URL", ((EditText)findViewById(R.id.editRefUrl)).getText().toString());
                intent.putExtra("GROUP", groupId);
                intent.putExtra("MEMO", ((EditText)findViewById(R.id.editRefMemo)).getText().toString());
                startActivity(intent);
                finish();
                break;
            case R.id.action_edit:
                // 入力画面を生成
                intent = new Intent(ReferencePasswordActivity.this, InputPasswordActivity.class);
                // 選択アイテムを編集モードで設定
                intent.putExtra("ID", id);
                intent.putExtra("EDIT", true);
                intent.putExtra("TITLE", getTitle().toString());
                intent.putExtra("ACCOUNT", ((EditText)findViewById(R.id.editRefAccount)).getText().toString());
                intent.putExtra("PASSWORD", ((EditText)findViewById(R.id.editRefPassword)).getText().toString());
                intent.putExtra("URL", ((EditText)findViewById(R.id.editRefUrl)).getText().toString());
                intent.putExtra("GROUP", groupId);
                intent.putExtra("MEMO", ((EditText)findViewById(R.id.editRefMemo)).getText().toString());
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();

        // 背景色を設定する
        findViewById(R.id.referencePasswordView).setBackgroundColor(loginDataManager.getBackgroundColor());
        // テキストサイズを設定する
        setTextSize(loginDataManager.getTextSize());
    }

    // クリップボードにコピーする処理
    public void CopyClipBoard(View view, String allText) {
        // クリップボードへの格納成功時は成功メッセージをトーストで表示
        boolean result = SetClipData(allText);
        if(result) {
            Snackbar.make(view, getString(R.string.copy_clipboard_success), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        } else {
            Snackbar.make(view, getString(R.string.copy_clipboard_failure), Snackbar.LENGTH_LONG).setAction("Action", null).show();
        }
    }

    // テキストデータをクリップボードに格納する
    private boolean SetClipData(String allText) {
        try {
            //クリップボードに格納するItemを作成
            ClipData.Item item = new ClipData.Item(allText);

            //MIMETYPEの作成
            String[] mimeType = new String[1];
            mimeType[0] = ClipDescription.MIMETYPE_TEXT_PLAIN;

            //クリップボードに格納するClipDataオブジェクトの作成
            ClipData cd = new ClipData(new ClipDescription("text_data", mimeType), item);

            //クリップボードにデータを格納
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(cd);
            return true;
        }
        catch(Exception e) {
            return false;
        }
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
        ((TextView)findViewById(R.id.accountRefView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editRefAccount)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.passwordRefView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editRefPassword)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.urlRefView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editRefUrl)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((TextView)findViewById(R.id.groupRefView)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size-3);
        ((EditText)findViewById(R.id.editRefGroup)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        ((EditText)findViewById(R.id.editRefMemo)).setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
    }
}
