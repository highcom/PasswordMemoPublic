package com.highcom.passwordmemo;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.highcom.passwordmemo.database.ListDataManager;
import com.highcom.passwordmemo.ui.DividerItemDecoration;
import com.highcom.passwordmemo.ui.list.ListViewAdapter;
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper;
import com.highcom.passwordmemo.util.login.LoginDataManager;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.co.recruit_mp.android.rmp_appirater.RmpAppirater;

public class PasswordListActivity extends AppCompatActivity implements ListViewAdapter.AdapterListener, SimpleCallbackHelper.SimpleCallbackListener {
    private static final int EDIT_DATA=1001;
    private static final int START_SETTING=1002;
    private static final int START_GROUP=1003;

    private String selectGroupName;
    private LoginDataManager loginDataManager;
    private ListDataManager listDataManager;
    private SimpleCallbackHelper simpleCallbackHelper;
    private FrameLayout adContainerView;
    private AdView mAdView;
    public RecyclerView recyclerView;
    public ListViewAdapter adapter;

    private Menu menu;
    private int currentMenuSelect;
    private Boolean currentMemoVisible;
    public String seachViewWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_password_list);

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) { }
        });
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList(getString(R.string.admob_test_device), getString(R.string.admob_test_device_xaomi))).build());
        adContainerView = findViewById(R.id.adView_frame);
        adContainerView.post(new Runnable() {
            @Override
            public void run() {
                loadBanner();
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // レビュー評価依頼のダイアログに表示する内容を設定
        RmpAppirater.Options options = new RmpAppirater.Options(
                getString(R.string.review_dialig_title),
                getString(R.string.review_dialig_message),
                getString(R.string.review_dialig_rate),
                getString(R.string.review_dialig_rate_later),
                getString(R.string.review_dialig_rate_cancel)
        );
        RmpAppirater.appLaunched(this,
            new RmpAppirater.ShowRateDialogCondition() {
                @Override
                public boolean isShowRateDialog(
                    long appLaunchCount, long appThisVersionCodeLaunchCount,
                    long firstLaunchDate, int appVersionCode,
                    int previousAppVersionCode, Date rateClickDate,
                    Date reminderClickDate, boolean doNotShowAgain) {

                    // レビュー依頼の文言を変えたバージョンでは、まだレビューをしておらず
                    // 長く利用していてバージョンアップしたユーザーに最初に一度だけ必ず表示する
                    if (appVersionCode == 21 && rateClickDate == null && appLaunchCount > 30 && appThisVersionCodeLaunchCount == 1) {
                        return true;
                    }

                    // 現在のアプリのバージョンで7回以上起動したか
                    if (appThisVersionCodeLaunchCount < 7) {
                        return false;
                    }
                    // ダイアログで「いいえ」を選択していないか
                    if (doNotShowAgain) {
                        return false;
                    }
                    // ユーザーがまだ評価していないか
                    if (rateClickDate != null) {
                        return false;
                    }
                    // ユーザーがまだ「あとで」を選択していないか
                    if (reminderClickDate != null) {
                        // 「あとで」を選択してから5日以上経過しているか
                        long prevtime = reminderClickDate.getTime();
                        long nowtime = new Date().getTime();
                        long diffDays = (nowtime - prevtime) / (1000 * 60 * 60 * 24);
                        if (diffDays < 5) {
                            return false;
                        }
                    }

                    return true;
                }
            }, options
        );

        loginDataManager = LoginDataManager.getInstance(this);
        listDataManager = ListDataManager.getInstance(this);

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        currentMemoVisible = loginDataManager.isMemoVisibleSwitchEnable();

        listDataManager.setSelectGroupId(loginDataManager.getSelectGroup());
        listDataManager.sortListData(loginDataManager.getSortKey());
        adapter = new ListViewAdapter(
                this,
                listDataManager.getDataList(),
                loginDataManager,
                this);
        adapter.setTextSize(loginDataManager.getTextSize());

        recyclerView = (RecyclerView) findViewById(R.id.passwordListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // セル間に区切り線を実装する
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        final float scale = getResources().getDisplayMetrics().density;
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper = new SimpleCallbackHelper(getApplicationContext(), recyclerView, scale, this) {
            @SuppressLint("ResourceType")
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                if (viewHolder.itemView.getId() == R.id.row_footer) return;

                underlayButtons.add(new SimpleCallbackHelper.UnderlayButton(
                        getString(R.string.delete),
                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete),
                        Color.parseColor(getString(R.color.underlay_red)),
                        (ListViewAdapter.ViewHolder) viewHolder,
                        new SimpleCallbackHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(RecyclerView.ViewHolder holder, int pos) {
                                new AlertDialog.Builder(PasswordListActivity.this)
                                        .setTitle(getString(R.string.delete_title_head) + ((ListViewAdapter.ViewHolder)holder).title.getText().toString() + getString(R.string.delete_title))
                                        .setMessage(getString(R.string.delete_message))
                                        .setPositiveButton(getString(R.string.delete_execute), (dialog1, which) -> {
                                            listDataManager.deleteData(((ListViewAdapter.ViewHolder)holder).id.toString());
                                            simpleCallbackHelper.resetSwipePos();
                                            reflesh();
                                        })
                                        .setNegativeButton(getString(R.string.delete_cancel), null)
                                        .show();
                            }
                        }
                ));
                underlayButtons.add(new SimpleCallbackHelper.UnderlayButton(
                        getString(R.string.edit),
                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_edit),
                        Color.parseColor(getString(R.color.underlay_gray)),
                        (ListViewAdapter.ViewHolder) viewHolder,
                        new SimpleCallbackHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(RecyclerView.ViewHolder holder, int pos) {
                                // 入力画面を生成
                                Intent intent = new Intent(PasswordListActivity.this, InputPasswordActivity.class);
                                // 選択アイテムを編集モードで設定
                                intent.putExtra("ID", ((ListViewAdapter.ViewHolder)holder).id);
                                intent.putExtra("EDIT", true);
                                intent.putExtra("TITLE", ((ListViewAdapter.ViewHolder)holder).title.getText().toString());
                                intent.putExtra("ACCOUNT", ((ListViewAdapter.ViewHolder)holder).account);
                                intent.putExtra("PASSWORD", ((ListViewAdapter.ViewHolder)holder).password);
                                intent.putExtra("URL", ((ListViewAdapter.ViewHolder)holder).url);
                                intent.putExtra("GROUP", ((ListViewAdapter.ViewHolder)holder).groupId);
                                intent.putExtra("MEMO", ((ListViewAdapter.ViewHolder)holder).memo);
                                startActivityForResult(intent, EDIT_DATA);
                            }
                        }
                ));
                underlayButtons.add(new SimpleCallbackHelper.UnderlayButton(
                        getString(R.string.copy),
                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_copy),
                        Color.parseColor(getString(R.color.underlay_gray)),
                        (ListViewAdapter.ViewHolder) viewHolder,
                        new SimpleCallbackHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(RecyclerView.ViewHolder holder, int pos) {
                                // 入力画面を生成
                                Intent intent = new Intent(PasswordListActivity.this, InputPasswordActivity.class);
                                // 選択アイテムを複製モードで設定
                                intent.putExtra("ID", listDataManager.getNewId());
                                intent.putExtra("EDIT", false);
                                intent.putExtra("TITLE", ((ListViewAdapter.ViewHolder)holder).title.getText().toString() + " " + getString(R.string.copy_title));
                                intent.putExtra("ACCOUNT", ((ListViewAdapter.ViewHolder)holder).account);
                                intent.putExtra("PASSWORD", ((ListViewAdapter.ViewHolder)holder).password);
                                intent.putExtra("URL", ((ListViewAdapter.ViewHolder)holder).url);
                                intent.putExtra("GROUP", ((ListViewAdapter.ViewHolder)holder).groupId);
                                intent.putExtra("MEMO", ((ListViewAdapter.ViewHolder)holder).memo);
                                startActivityForResult(intent, EDIT_DATA);
                            }
                        }
                ));
            }
        };

        // フローティングボタンからの新規追加処理
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PasswordListActivity.this, InputPasswordActivity.class);
                intent.putExtra("ID", listDataManager.getNewId());
                intent.putExtra("EDIT", false);
                startActivityForResult(intent, EDIT_DATA);
            }
        });

        // 渡されたデータを取得する
        Intent intent = getIntent();
        boolean first_time = intent.getBooleanExtra("FIRST_TIME", false);
        if (first_time) {
            intent.putExtra("FIRST_TIME", false);
            operationInstructionDialog();
        }
    }

    private void loadBanner() {
        // Create an ad request.
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.admob_unit_id_1));
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
        getMenuInflater().inflate(R.menu.menu_list, menu);
        this.menu = menu;
        switch (loginDataManager.getSortKey()) {
            case ListDataManager.SORT_ID:
            default:
                currentMenuSelect = R.id.sort_default;
                break;
            case ListDataManager.SORT_TITLE:
                currentMenuSelect = R.id.sort_title;
                break;
            case ListDataManager.SORT_INPUTDATE:
                currentMenuSelect = R.id.sort_update;
                break;
        }
        // 現在選択されている選択アイコンを設定する
        String selectMenuTitle = menu.findItem(currentMenuSelect).getTitle().toString().replace(getString(R.string.no_select_menu_icon), getString(R.string.select_menu_icon));
        menu.findItem(currentMenuSelect).setTitle(selectMenuTitle);
        MenuItem searchMenuItem = menu.findItem(R.id.menu_search_view);
        SearchView searchView = (SearchView)searchMenuItem.getActionView();
        SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        searchAutoComplete.setHintTextColor(Color.rgb(0xff, 0xff, 0xff));
        searchAutoComplete.setHint(getString(R.string.search_text_message));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                seachViewWord = newText;
                setSearchWordFilter();
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 編集状態は解除する
                adapter.setEditEnable(false);
                finish();
                break;
            case R.id.edit_mode:
                // 編集状態の変更
                listDataManager.sortListData(ListDataManager.SORT_ID);
                if (adapter.getEditEnable()) {
                    setCurrentSelectMenuTitle(menu.findItem(R.id.sort_default), R.id.sort_default);
                    adapter.setEditEnable(false);
                } else {
                    setCurrentSelectMenuTitle(item, R.id.edit_mode);
                    adapter.setEditEnable(true);
                }
                setTitle(getString(R.string.sort_name_default) + "：" + selectGroupName);
                recyclerView.setAdapter(adapter);
                loginDataManager.setSortKey(ListDataManager.SORT_ID);
                break;
            case R.id.sort_default:
                setCurrentSelectMenuTitle(item, R.id.sort_default);
                setTitle(getString(R.string.sort_name_default) + "：" + selectGroupName);
                listDataManager.sortListData(ListDataManager.SORT_ID);
                if (adapter.getEditEnable()) adapter.setEditEnable(false);
                recyclerView.setAdapter(adapter);
                loginDataManager.setSortKey(ListDataManager.SORT_ID);
                break;
            case R.id.sort_title:
                setCurrentSelectMenuTitle(item, R.id.sort_title);
                setTitle(getString(R.string.sort_name_title) + "：" + selectGroupName);
                listDataManager.sortListData(ListDataManager.SORT_TITLE);
                if (adapter.getEditEnable()) adapter.setEditEnable(false);
                recyclerView.setAdapter(adapter);
                loginDataManager.setSortKey(ListDataManager.SORT_TITLE);
                break;
            case R.id.sort_update:
                setCurrentSelectMenuTitle(item, R.id.sort_update);
                setTitle(getString(R.string.sort_name_update) + "：" + selectGroupName);
                listDataManager.sortListData(ListDataManager.SORT_INPUTDATE);
                if (adapter.getEditEnable()) adapter.setEditEnable(false);
                recyclerView.setAdapter(adapter);
                loginDataManager.setSortKey(ListDataManager.SORT_INPUTDATE);
                break;
            case R.id.select_group:
                // 設定画面へ遷移
                Intent intentGroup = new Intent(PasswordListActivity.this, GroupListActivity.class);
                startActivityForResult(intentGroup, START_GROUP);
                break;
            case R.id.setting_menu:
                // 設定画面へ遷移
                Intent intent = new Intent(PasswordListActivity.this, SettingActivity.class);
                startActivityForResult(intent, START_SETTING);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setCurrentSelectMenuTitle(MenuItem item, int id) {
        // 現在選択されているメニューの選択アイコンを戻す
        String currentMenuTitle = menu.findItem(currentMenuSelect).getTitle().toString().replace(getString(R.string.select_menu_icon), getString(R.string.no_select_menu_icon));
        menu.findItem(currentMenuSelect).setTitle(currentMenuTitle);
        // 今回選択されたメニューに選択アイコンを設定する
        currentMenuSelect = id;
        String selectMenuTitle = item.getTitle().toString().replace(getString(R.string.no_select_menu_icon), getString(R.string.select_menu_icon));
        item.setTitle(selectMenuTitle);
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();
        // 背景色を設定する
        ((ConstraintLayout)findViewById(R.id.passwordListActivityView)).setBackgroundColor(loginDataManager.getBackgroundColor());
        selectGroupName = getString(R.string.list_title);
        boolean isSelectGroupExist = false;
        for (Map<String, String> group : listDataManager.getGroupList()) {
            if (Long.valueOf(group.get("group_id")) == loginDataManager.getSelectGroup()) {
                selectGroupName = group.get("name");
                listDataManager.setSelectGroupId(loginDataManager.getSelectGroup());
                isSelectGroupExist = true;
                break;
            }
        }
        // 選択していたグループが存在しなくなった場合には「すべて」にリセットする
        if (!isSelectGroupExist) {
            loginDataManager.setSelectGroup(1L);
            listDataManager.setSelectGroupId(1L);
        }
        switch (loginDataManager.getSortKey()) {
            case ListDataManager.SORT_ID:
            default:
                setTitle(getString(R.string.sort_name_default) + "：" + selectGroupName);
                break;
            case ListDataManager.SORT_TITLE:
                setTitle(getString(R.string.sort_name_title) + "：" + selectGroupName);
                break;
            case ListDataManager.SORT_INPUTDATE:
                setTitle(getString(R.string.sort_name_update) + "：" + selectGroupName);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean needRefresh = false;
        // メモ表示設定に変更があった場合には再描画する
        if (currentMemoVisible != loginDataManager.isMemoVisibleSwitchEnable()) {
            currentMemoVisible = loginDataManager.isMemoVisibleSwitchEnable();
            needRefresh = true;
        }
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter.getTextSize() != loginDataManager.getTextSize()) {
            adapter.setTextSize(loginDataManager.getTextSize());
            needRefresh = true;
        }

        if (needRefresh) reflesh();
    }

    private void operationInstructionDialog() {
        final android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.operation_opening_title)
                .setView(getLayoutInflater().inflate(R.layout.alert_operating_instructions, null))
                .setPositiveButton(R.string.close, null)
                .create();
        alertDialog.show();
        alertDialog.findViewById(R.id.operation_instruction_message).setVisibility(View.VISIBLE);
        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    // 入力文字列に応じてフィルタをする
    public void setSearchWordFilter() {
        Filter filter = ((Filterable) recyclerView.getAdapter()).getFilter();
        if (TextUtils.isEmpty(seachViewWord)) {
            filter.filter(null);
        } else {
            filter.filter(seachViewWord.toLowerCase());
        }
    }

    // データの一覧を更新する
    public void reflesh() {
        adapter.notifyDataSetChanged();
        // フィルタしている場合はフィルタデータの一覧も更新する
        setSearchWordFilter();
    }

    @Override
    public void onDestroy() {
        listDataManager.closeData();
        if (mAdView != null) mAdView.destroy();
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager.isDisplayBackgroundSwitchEnable() && PasswordMemoLifecycle.getIsBackground()) {
            finishAffinity();
        }
        super.onDestroy();
    }

    @Override
    public void onAdapterClicked(View view) {
        // 編集状態の場合は入力画面に遷移しない
        if (adapter.getEditEnable() == true) {
            return;
        }
        // 入力画面を生成
        Intent intent = new Intent(PasswordListActivity.this, ReferencePasswordActivity.class);
        // 選択アイテムを設定
        ListViewAdapter.ViewHolder holder = (ListViewAdapter.ViewHolder) view.getTag();
        intent.putExtra("ID", holder.id.longValue());
        intent.putExtra("TITLE", holder.title.getText().toString());
        intent.putExtra("ACCOUNT", holder.account);
        intent.putExtra("PASSWORD", holder.password);
        intent.putExtra("URL", holder.url);
        intent.putExtra("GROUP", holder.groupId);
        intent.putExtra("MEMO", holder.memo);
        startActivityForResult(intent, EDIT_DATA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_DATA || requestCode == START_GROUP || (requestCode == START_SETTING && resultCode == SettingActivity.NEED_UPDATE)) {
            if (requestCode == START_GROUP) {
                for (Map<String, String> group : listDataManager.getGroupList()) {
                    if (Long.valueOf(group.get("group_id")) == loginDataManager.getSelectGroup()) {
                        selectGroupName = group.get("name");
                        switch (loginDataManager.getSortKey()) {
                            case ListDataManager.SORT_ID:
                            default:
                                setTitle(getString(R.string.sort_name_default) + "：" + selectGroupName);
                                break;
                            case ListDataManager.SORT_TITLE:
                                setTitle(getString(R.string.sort_name_title) + "：" + selectGroupName);
                                break;
                            case ListDataManager.SORT_INPUTDATE:
                                setTitle(getString(R.string.sort_name_update) + "：" + selectGroupName);
                                break;
                        }
                        break;
                    }
                }
            }
            reflesh();
        }
    }

    @Override
    public boolean onSimpleCallbackMove(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (adapter.getEditEnable() && TextUtils.isEmpty(seachViewWord)) {
            final int fromPos = viewHolder.getAdapterPosition();
            final int toPos = target.getAdapterPosition();
            adapter.notifyItemMoved(fromPos, toPos);
            listDataManager.rearrangeData(fromPos, toPos);
            return true;
        }
        return false;
    }

    @Override
    public void clearSimpleCallbackView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        recyclerView.setAdapter(adapter);
    }
}
