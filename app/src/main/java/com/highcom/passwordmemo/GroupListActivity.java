package com.highcom.passwordmemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
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
import com.highcom.passwordmemo.ui.list.GroupListAdapter;
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper;
import com.highcom.passwordmemo.util.login.LoginDataManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupListActivity extends AppCompatActivity implements GroupListAdapter.GroupAdapterListener, SimpleCallbackHelper.SimpleCallbackListener {

    private LoginDataManager loginDataManager;
    private ListDataManager listDataManager;
    private FrameLayout adContainerView;
    private AdView mAdView;
    public RecyclerView recyclerView;
    private FloatingActionButton groupFab;
    public GroupListAdapter adapter;
    private SimpleCallbackHelper simpleCallbackHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);


        loginDataManager = LoginDataManager.getInstance(this);
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) { }
        });
        MobileAds.setRequestConfiguration(
                new RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("874848BA4D9A6B9B0A256F7862A47A31", "A02A04D245766C519D07D09F0E258E1E")).build());
        adContainerView = findViewById(R.id.adView_groupFrame);
        adContainerView.post(new Runnable() {
            @Override
            public void run() {
                loadBanner();
            }
        });

        setTitle(getString(R.string.group_title) + getString(R.string.group_title_select));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loginDataManager = LoginDataManager.getInstance(this);
        listDataManager = ListDataManager.getInstance(this);

        // グループ名が空白のデータが存在していた場合には削除する
        List<Map<String, String>> groupList = listDataManager.getGroupList();
        for (Map<String, String> group : groupList) {
            if (group.get("name").equals("")) {
                listDataManager.deleteGroupData(group.get("group_id"));
                listDataManager.resetGroupIdData(Long.valueOf(group.get("group_id")));
            }
        }
        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.isDisplayBackgroundSwitchEnable()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }

        adapter = new GroupListAdapter(
                this,
                listDataManager.getGroupList(),
                this);
        adapter.setTextSize(loginDataManager.getTextSize());

        recyclerView = (RecyclerView) findViewById(R.id.groupListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        // セル間に区切り線を実装する
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);

        groupFab = findViewById(R.id.groupFab);
        groupFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, String> data = new HashMap<String, String>();
                data.put("group_id", Long.valueOf(listDataManager.getNewGroupId()).toString());
                data.put("group_order", Integer.valueOf(listDataManager.getGroupList().size() + 1).toString());
                data.put("name", "");
                listDataManager.setGroupData(false, data);
                adapter.notifyDataSetChanged();
                // 新規作成時は対象のセルにフォーカスされるようにスクロールする
                for (int position = 0; position < listDataManager.getGroupList().size(); position++) {
                    if (listDataManager.getGroupList().get(position).get("name").equals("")) {
                        recyclerView.smoothScrollToPosition(position);
                        break;
                    }
                }
            }
        });
        if (!adapter.getEditEnable()) groupFab.setVisibility(View.GONE);

        final float scale = getResources().getDisplayMetrics().density;
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper = new SimpleCallbackHelper(getApplicationContext(), recyclerView, scale, this) {
            @SuppressLint("ResourceType")
            @Override
            public void instantiateUnderlayButton(RecyclerView.ViewHolder viewHolder, List<UnderlayButton> underlayButtons) {
                if (viewHolder.itemView.getId() == R.id.row_footer) return;
                // グループIDの1はすべてなので削除出来ないようにする
                if (((GroupListAdapter.GroupViewHolder)viewHolder).groupId == 1L) return;

                underlayButtons.add(new SimpleCallbackHelper.UnderlayButton(
                        getString(R.string.delete),
                        BitmapFactory.decodeResource(getResources(), R.drawable.ic_delete),
                        Color.parseColor(getString(R.color.underlay_red)),
                        (GroupListAdapter.GroupViewHolder) viewHolder,
                        new SimpleCallbackHelper.UnderlayButtonClickListener() {
                            @Override
                            public void onClick(RecyclerView.ViewHolder holder, int pos) {
                                new AlertDialog.Builder(GroupListActivity.this)
                                        .setTitle(getString(R.string.delete_title_head) + ((GroupListAdapter.GroupViewHolder)holder).groupName.getText().toString() + getString(R.string.delete_title))
                                        .setMessage(getString(R.string.delete_message))
                                        .setPositiveButton(getString(R.string.delete_execute), (dialog1, which) -> {
                                            listDataManager.deleteGroupData(((GroupListAdapter.GroupViewHolder)holder).groupId.toString());
                                            listDataManager.resetGroupIdData(((GroupListAdapter.GroupViewHolder)holder).groupId);
                                            if (loginDataManager.getSelectGroup() == ((GroupListAdapter.GroupViewHolder)holder).groupId) {
                                                loginDataManager.setSelectGroup(1L);
                                                listDataManager.setSelectGroupId(1L);
                                            }
                                            simpleCallbackHelper.resetSwipePos();
                                            adapter.notifyDataSetChanged();
                                        })
                                        .setNegativeButton(getString(R.string.delete_cancel), null)
                                        .show();
                            }
                        }
                ));
            }
        };
    }

    private void loadBanner() {
        // Create an ad request.
        mAdView = new AdView(this);
        mAdView.setAdUnitId(getString(R.string.admob_unit_id_4));
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

    @SuppressLint("ResourceType")
    @Override
    protected void onStart() {
        super.onStart();
        // 背景色を設定する
        ((ConstraintLayout)findViewById(R.id.groupListActivityView)).setBackgroundColor(loginDataManager.getBackgroundColor());
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean needRefresh = false;
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter.getTextSize() != loginDataManager.getTextSize()) {
            adapter.setTextSize(loginDataManager.getTextSize());
            needRefresh = true;
        }

        if (needRefresh) adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_mode, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                recyclerView.setAdapter(adapter);
                finish();
                break;
            case R.id.select_group_mode:
                if (adapter.getEditEnable()) {
                    adapter.setEditEnable(false);
                    setTitle(getString(R.string.group_title) + getString(R.string.group_title_select));
                    groupFab.setVisibility(View.GONE);
                } else {
                    adapter.setEditEnable(true);
                    setTitle(getString(R.string.group_title) + getString(R.string.group_title_edit));
                    groupFab.setVisibility(View.VISIBLE);
                }
                recyclerView.setAdapter(adapter);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mAdView != null) mAdView.destroy();
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager.isDisplayBackgroundSwitchEnable() && PasswordMemoLifecycle.getIsBackground()) {
            listDataManager.closeData();
            finishAffinity();
        }
        super.onDestroy();
    }

    @Override
    public void onGroupNameClicked(View view, Long groupId) {
        if (adapter.getEditEnable()) {
            view.post(() -> {
                groupFab.setVisibility(View.GONE);
                view.setFocusable(true);
                view.setFocusableInTouchMode(true);
                view.requestFocus();
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (inputMethodManager != null) {
                    inputMethodManager.showSoftInput(view, 0);
                }
            });
        } else {
            loginDataManager.setSelectGroup(groupId);
            listDataManager.setSelectGroupId(groupId);
            finish();
        }
    }

    @Override
    public void onGroupNameOutOfFocused(View view, Map<String, String> data) {
        // 内容編集中にフォーカスが外れた場合は、キーボードを閉じる
        InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

        groupFab.setVisibility(View.VISIBLE);
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        view.requestFocus();
        // 内容が空白の場合には削除する
        if (data.get("name").equals("")) {
            listDataManager.deleteGroupData(data.get("group_id"));
            listDataManager.resetGroupIdData(Long.valueOf(data.get("group_id")));
            if (loginDataManager.getSelectGroup() == Long.parseLong(data.get("group_id"))) {
                loginDataManager.setSelectGroup(1L);
                listDataManager.setSelectGroupId(1L);
            }
            return;
        }
        listDataManager.setGroupData(true, data);
    }

    @Override
    public boolean onSimpleCallbackMove(RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        if (!adapter.getEditEnable()) return false;
        // グループ名が入力されていない場合は移動させない
        if (((GroupListAdapter.GroupViewHolder)viewHolder).groupName.getText().toString().equals("")
                || ((GroupListAdapter.GroupViewHolder)target).groupName.getText().toString().equals("")) return false;
        // グループ名が入力途中でDB反映されていないデータも並び替えさせない
        List<Map<String, String>> groupList = listDataManager.getGroupList();
        for (Map<String, String> group : groupList) {
            if (group.get("group_id").equals(((GroupListAdapter.GroupViewHolder)viewHolder).groupId.toString())
                || group.get("group_id").equals(((GroupListAdapter.GroupViewHolder)target).groupId.toString())) {
                if (group.get("name").equals("")) return false;
            }
        }
        final int fromPos = viewHolder.getAdapterPosition();
        final int toPos = target.getAdapterPosition();
        // 1番目のデータは「すべて」なので並べ替え不可にする
        if (fromPos == 0 || toPos == 0) return false;
        adapter.notifyItemMoved(fromPos, toPos);
        listDataManager.rearrangeGroupData(fromPos, toPos);
        return true;
    }

    @Override
    public void clearSimpleCallbackView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        recyclerView.setAdapter(adapter);
    }
}