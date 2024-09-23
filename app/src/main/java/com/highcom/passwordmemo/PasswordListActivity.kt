package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Filterable
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.highcom.passwordmemo.ui.DividerItemDecoration
import com.highcom.passwordmemo.ui.list.PasswordListAdapter
import com.highcom.passwordmemo.ui.list.PasswordListAdapter.AdapterListener
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper.SimpleCallbackListener
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import jp.co.recruit_mp.android.rmp_appirater.RmpAppirater
import jp.co.recruit_mp.android.rmp_appirater.RmpAppirater.ShowRateDialogCondition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale

/**
 * パスワード一覧画面アクティビティ
 *
 */
class PasswordListActivity : AppCompatActivity(), AdapterListener {
    /** 選択グループ名称 */
    private var selectGroupName: String? = null
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** スワイプボタン表示用通知ヘルパー */
    private var simpleCallbackHelper: SimpleCallbackHelper? = null
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** 広告ビュー */
    private var mAdView: AdView? = null
    /** パスワード一覧表示用リサイクラービュー */
    var recyclerView: RecyclerView? = null
    /** パスワード一覧用アダプタ */
    var adapter: PasswordListAdapter? = null
    /** 操作メニュー */
    private var menu: Menu? = null
    /** 現在洗濯中のメニュー */
    private var currentMenuSelect = 0
    /** 現在のメモ表示設定 */
    private var currentMemoVisible: Boolean? = null
    /** 検索文字列 */
    var seachViewWord: String? = null
    /** パスワード一覧ビューモデル */
    private val passwordListViewModel: PasswordListViewModel by viewModels {
        PasswordListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }

    @Suppress("DEPRECATION")
    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_list)
        MobileAds.initialize(this) { }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                listOf(
                    getString(R.string.admob_test_device),
                    getString(R.string.admob_test_device_xaomi)
                )
            ).build()
        )
        adContainerView = findViewById(R.id.adView_frame)
        adContainerView?.post { loadBanner() }
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        // レビュー評価依頼のダイアログに表示する内容を設定
        val options = RmpAppirater.Options(
            getString(R.string.review_dialig_title),
            getString(R.string.review_dialig_message),
            getString(R.string.review_dialig_rate),
            getString(R.string.review_dialig_rate_later),
            getString(R.string.review_dialig_rate_cancel)
        )
        RmpAppirater.appLaunched(
            this,
            ShowRateDialogCondition { appLaunchCount, appThisVersionCodeLaunchCount, _, appVersionCode, _, rateClickDate, reminderClickDate, doNotShowAgain ->
                // レビュー依頼の文言を変えたバージョンでは、まだレビューをしておらず
                // 長く利用していてバージョンアップしたユーザーに最初に一度だけ必ず表示する
                if (appVersionCode == 21 && rateClickDate == null && appLaunchCount > 30 && appThisVersionCodeLaunchCount == 1L) {
                    return@ShowRateDialogCondition true
                }

                // 現在のアプリのバージョンで7回以上起動したか
                if (appThisVersionCodeLaunchCount < 7) {
                    return@ShowRateDialogCondition false
                }
                // ダイアログで「いいえ」を選択していないか
                if (doNotShowAgain) {
                    return@ShowRateDialogCondition false
                }
                // ユーザーがまだ評価していないか
                if (rateClickDate != null) {
                    return@ShowRateDialogCondition false
                }
                // ユーザーがまだ「あとで」を選択していないか
                if (reminderClickDate != null) {
                    // 「あとで」を選択してから5日以上経過しているか
                    val prevtime = reminderClickDate.time
                    val nowtime = Date().time
                    val diffDays = (nowtime - prevtime) / (1000 * 60 * 60 * 24)
                    if (diffDays < 5) {
                        return@ShowRateDialogCondition false
                    }
                }
                true
            }, options
        )
        loginDataManager = LoginDataManager.getInstance(application)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        currentMemoVisible = loginDataManager!!.memoVisibleSwitchEnable
        adapter = PasswordListAdapter(
            this,
            loginDataManager,
            this
        )
        adapter?.textSize = loginDataManager!!.textSize
        recyclerView = findViewById<View>(R.id.passwordListView) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter

        // 選択されているグループのパスワード一覧を設定する
        passwordListViewModel.setSelectGroup(loginDataManager?.selectGroup ?: 1L)
        lifecycleScope.launchWhenStarted {
            passwordListViewModel.passwordList.collect { list ->
                adapter?.setList(list)
                adapter?.sortPasswordList(loginDataManager?.sortKey)
                reflesh()
            }
        }

        // セル間に区切り線を実装する
        val itemDecoration: ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.Companion.VERTICAL_LIST)
        recyclerView!!.addItemDecoration(itemDecoration)
        val scale = resources.displayMetrics.density
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper =
            object : SimpleCallbackHelper(applicationContext, recyclerView, scale, PasswordListCallbackListener()) {
                @SuppressLint("ResourceType")
                override fun instantiateUnderlayButton(
                    viewHolder: RecyclerView.ViewHolder,
                    underlayButtons: MutableList<UnderlayButton>
                ) {
                    if (viewHolder.itemView.id == R.id.row_footer) return
                    underlayButtons.add(UnderlayButton(
                        getString(R.string.delete),
                        BitmapFactory.decodeResource(resources, R.drawable.ic_delete),
                        Color.parseColor(getString(R.color.underlay_red)),
                        viewHolder as PasswordListAdapter.ViewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                AlertDialog.Builder(this@PasswordListActivity)
                                    .setTitle(
                                        getString(R.string.delete_title_head) + (holder as PasswordListAdapter.ViewHolder).title?.text.toString() + getString(
                                            R.string.delete_title
                                        )
                                    )
                                    .setMessage(getString(R.string.delete_message))
                                    .setPositiveButton(getString(R.string.delete_execute)) { _: DialogInterface?, _: Int ->
                                        holder.id?.let { passwordListViewModel.delete(it) }
                                        simpleCallbackHelper!!.resetSwipePos()
                                        reflesh()
                                    }
                                    .setNegativeButton(getString(R.string.delete_cancel), null)
                                    .show()
                            }
                        }
                    ))
                    underlayButtons.add(UnderlayButton(
                        getString(R.string.edit),
                        BitmapFactory.decodeResource(resources, R.drawable.ic_edit),
                        Color.parseColor(getString(R.color.underlay_gray)),
                        viewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                val intent =
                                    Intent(this@PasswordListActivity, InputPasswordActivity::class.java)
                                // 選択アイテムを編集モードで設定
                                intent.putExtra("ID", (holder as PasswordListAdapter.ViewHolder).id)
                                intent.putExtra("EDIT", true)
                                intent.putExtra(
                                    "TITLE",
                                    holder.title?.text.toString()
                                )
                                intent.putExtra(
                                    "ACCOUNT",
                                    holder.account
                                )
                                intent.putExtra(
                                    "PASSWORD",
                                    holder.password
                                )
                                intent.putExtra("URL", holder.url)
                                intent.putExtra("GROUP", holder.groupId)
                                intent.putExtra("MEMO", holder.memo)
                                startActivityForResult(intent, EDIT_DATA)
                            }
                        }
                    ))
                    underlayButtons.add(UnderlayButton(
                        getString(R.string.copy),
                        BitmapFactory.decodeResource(resources, R.drawable.ic_copy),
                        Color.parseColor(getString(R.color.underlay_gray)),
                        viewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                val intent =
                                    Intent(this@PasswordListActivity, InputPasswordActivity::class.java)
                                // 選択アイテムを複製モードで設定
                                intent.putExtra("EDIT", false)
                                intent.putExtra(
                                     "TITLE",
                                     (holder as PasswordListAdapter.ViewHolder).title?.text.toString() + " " + getString(
                                        R.string.copy_title
                                    )
                                )
                                intent.putExtra(
                                     "ACCOUNT",
                                    holder.account
                                )
                                intent.putExtra(
                                     "PASSWORD",
                                    holder.password
                                )
                                intent.putExtra("URL", holder.url)
                                intent.putExtra("GROUP", holder.groupId)
                                intent.putExtra("MEMO", holder.memo)
                                startActivityForResult(intent, EDIT_DATA)
                            }
                        }
                    ))
                }
            }

        // フローティングボタンからの新規追加処理
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this@PasswordListActivity, InputPasswordActivity::class.java)
            intent.putExtra("EDIT", false)
            startActivityForResult(intent, EDIT_DATA)
        }

        // 渡されたデータを取得する
        val intent = intent
        val firstTime = intent.getBooleanExtra("FIRST_TIME", false)
        if (firstTime) {
            intent.putExtra("FIRST_TIME", false)
            operationInstructionDialog()
        }
    }

    /**
     * バナー広告ロード処理
     *
     */
    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(this)
        mAdView!!.adUnitId = getString(R.string.admob_unit_id_1)
        adContainerView!!.removeAllViews()
        adContainerView!!.addView(mAdView)
        val adSize = adSize
        mAdView!!.setAdSize(adSize)
        val adRequest = AdRequest.Builder().build()

        // Start loading the ad in the background.
        mAdView!!.loadAd(adRequest)
    }

    /** 広告サイズ設定 */
    @Suppress("DEPRECATION")
    private val adSize: AdSize
        get() {
            // Determine the screen width (less decorations) to use for the ad width.
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)
            val density = outMetrics.density
            var adWidthPixels = adContainerView!!.width.toFloat()

            // If the ad hasn't been laid out, default to the full screen width.
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        this.menu = menu
        currentMenuSelect = when (loginDataManager!!.sortKey) {
            PasswordListAdapter.SORT_ID -> R.id.sort_default
            PasswordListAdapter.SORT_TITLE -> R.id.sort_title
            PasswordListAdapter.SORT_INPUTDATE -> R.id.sort_update
            else -> R.id.sort_default
        }
        // 現在選択されている選択アイコンを設定する
        val selectMenuTitle = menu.findItem(currentMenuSelect).title.toString()
            .replace(getString(R.string.no_select_menu_icon), getString(R.string.select_menu_icon))
        menu.findItem(currentMenuSelect).title = selectMenuTitle
        // 検索窓の動作を設定する
        val searchMenuItem = menu.findItem(R.id.menu_search_view)
        val searchView = searchMenuItem.actionView as SearchView?
        val searchAutoComplete =
            searchView!!.findViewById<SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
        searchAutoComplete.setHintTextColor(Color.rgb(0xff, 0xff, 0xff))
        searchAutoComplete.hint = getString(R.string.search_text_message)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                seachViewWord = newText
                setSearchWordFilter()
                return false
            }
        })
        return true
    }

    @Suppress("DEPRECATION")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // 戻るボタン
            android.R.id.home -> {
                // 編集状態は解除する
                adapter?.editEnable = false
                finish()
            }
            // 編集モード
            R.id.edit_mode -> {
                // 編集状態の変更
                adapter?.sortPasswordList(PasswordListAdapter.SORT_ID)
                if (adapter?.editEnable == true) {
                    setCurrentSelectMenuTitle(menu!!.findItem(R.id.sort_default), R.id.sort_default)
                    adapter?.editEnable = false
                } else {
                    setCurrentSelectMenuTitle(item, R.id.edit_mode)
                    adapter?.editEnable = true
                }
                title = getString(R.string.sort_name_default) + "：" + selectGroupName
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_ID)
            }
            // 標準ソート
            R.id.sort_default -> {
                setCurrentSelectMenuTitle(item, R.id.sort_default)
                title = getString(R.string.sort_name_default) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_ID)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_ID)
            }
            // タイトルソート
            R.id.sort_title -> {
                setCurrentSelectMenuTitle(item, R.id.sort_title)
                title = getString(R.string.sort_name_title) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_TITLE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_TITLE)
            }
            // 更新日ソート
            R.id.sort_update -> {
                setCurrentSelectMenuTitle(item, R.id.sort_update)
                title = getString(R.string.sort_name_update) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_INPUTDATE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_INPUTDATE)
            }
            // グループ選択
            R.id.select_group -> {
                // 設定画面へ遷移
                val intentGroup = Intent(this@PasswordListActivity, GroupListActivity::class.java)
                startActivityForResult(intentGroup, START_GROUP)
            }
            // 設定メニュー
            R.id.setting_menu -> {
                // 設定画面へ遷移
                val intent = Intent(this@PasswordListActivity, SettingActivity::class.java)
                startActivityForResult(intent, START_SETTING)
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * 現在選択されているメニューに合わせた表示設定処理
     *
     * @param item メニュー
     * @param id 選択メニューID
     */
    private fun setCurrentSelectMenuTitle(item: MenuItem, id: Int) {
        // 現在選択されているメニューの選択アイコンを戻す
        val currentMenuTitle = menu!!.findItem(currentMenuSelect).title.toString()
            .replace(getString(R.string.select_menu_icon), getString(R.string.no_select_menu_icon))
        menu!!.findItem(currentMenuSelect).title = currentMenuTitle
        // 今回選択されたメニューに選択アイコンを設定する
        currentMenuSelect = id
        val selectMenuTitle = item.title.toString()
            .replace(getString(R.string.no_select_menu_icon), getString(R.string.select_menu_icon))
        item.title = selectMenuTitle
    }

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        // 背景色を設定する
        (findViewById<View>(R.id.passwordListActivityView) as ConstraintLayout).setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
        selectGroupName = getString(R.string.list_title)
        var isSelectGroupExist = false
        lifecycleScope.launch {
            groupListViewModel.groupList.collect { list ->
                for (entity in list) {
                    if (entity.groupId == loginDataManager?.selectGroup) {
                        selectGroupName = entity.name
                        isSelectGroupExist = true
                    }
                }
                // 選択していたグループが存在しなくなった場合には「すべて」にリセットする
                if (!isSelectGroupExist) {
                    loginDataManager!!.setSelectGroup(1L)
                    passwordListViewModel.setSelectGroup(1L)
                }
                // タイトルに選択しているグループ名を設定
                title = when (loginDataManager!!.sortKey) {
                    PasswordListAdapter.SORT_ID -> getString(R.string.sort_name_default) + "：" + selectGroupName
                    PasswordListAdapter.SORT_TITLE -> getString(R.string.sort_name_title) + "：" + selectGroupName
                    PasswordListAdapter.SORT_INPUTDATE -> getString(R.string.sort_name_update) + "：" + selectGroupName
                    else -> getString(R.string.sort_name_default) + "：" + selectGroupName
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        var needRefresh = false
        // メモ表示設定に変更があった場合には再描画する
        if (currentMemoVisible != loginDataManager!!.memoVisibleSwitchEnable) {
            currentMemoVisible = loginDataManager!!.memoVisibleSwitchEnable
            needRefresh = true
        }
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter?.textSize != loginDataManager!!.textSize) {
            adapter?.textSize = loginDataManager!!.textSize
            needRefresh = true
        }
        if (needRefresh) reflesh()
    }

    /**
     * 操作説明ダイアログ生成処理
     *
     */
    @SuppressLint("InflateParams")
    private fun operationInstructionDialog() {
        val alertDialog = android.app.AlertDialog.Builder(this)
            .setTitle(R.string.operation_opening_title)
            .setView(layoutInflater.inflate(R.layout.alert_operating_instructions, null))
            .setPositiveButton(R.string.close, null)
            .create()
        alertDialog.show()
        alertDialog.findViewById<View>(R.id.operation_instruction_message).visibility = View.VISIBLE
        alertDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener { alertDialog.dismiss() }
    }

    /**
     * 検索文字列に応じたパスワード一覧のフィルタ処理
     *
     */
    fun setSearchWordFilter() {
        val filter = (recyclerView!!.adapter as Filterable?)!!.filter
        if (TextUtils.isEmpty(seachViewWord)) {
            filter.filter(null)
        } else {
            filter.filter(seachViewWord!!.lowercase(Locale.getDefault()))
        }
    }

    /**
     * パスワードデータ一覧更新処理
     *
     */
    @SuppressLint("NotifyDataSetChanged")
    fun reflesh() {
        adapter!!.notifyDataSetChanged()
        // フィルタしている場合はフィルタデータの一覧も更新する
        setSearchWordFilter()
    }

    public override fun onDestroy() {
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    /**
     * パスワード一覧の項目タップ時の処理
     *
     * @param view タップされた項目のビュー
     */
    @Suppress("DEPRECATION")
    override fun onAdapterClicked(view: View) {
        // 編集状態の場合は入力画面に遷移しない
        if (adapter?.editEnable == true) {
            return
        }
        // 入力画面を生成
        val intent = Intent(this@PasswordListActivity, ReferencePasswordActivity::class.java)
        // 選択アイテムを設定
        val holder = view.tag as PasswordListAdapter.ViewHolder
        intent.putExtra("ID", holder.id)
        intent.putExtra("TITLE", holder.title?.text.toString())
        intent.putExtra("ACCOUNT", holder.account)
        intent.putExtra("PASSWORD", holder.password)
        intent.putExtra("URL", holder.url)
        intent.putExtra("GROUP", holder.groupId)
        intent.putExtra("MEMO", holder.memo)
        startActivityForResult(intent, EDIT_DATA)
    }

    /**
     * 各画面から戻ってきた際の更新処理
     *
     * @param requestCode 要求コード
     * @param resultCode 結果コード
     * @param data 戻り値データ
     */
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_DATA || requestCode == START_GROUP || requestCode == START_SETTING && resultCode == SettingActivity.NEED_UPDATE) {
            if (requestCode == START_GROUP) {
                // 選択したグループを設定
                passwordListViewModel.setSelectGroup(loginDataManager?.selectGroup ?: 1L)
                lifecycleScope.launch {
                    groupListViewModel.groupList.collect {
                        for (group in it) {
                            if (group.groupId == loginDataManager!!.selectGroup) {
                                selectGroupName = group.name
                                title = when (loginDataManager!!.sortKey) {
                                    PasswordListAdapter.SORT_ID -> getString(R.string.sort_name_default) + "：" + selectGroupName
                                    PasswordListAdapter.SORT_TITLE -> getString(R.string.sort_name_title) + "：" + selectGroupName
                                    PasswordListAdapter.SORT_INPUTDATE -> getString(R.string.sort_name_update) + "：" + selectGroupName
                                    else -> getString(R.string.sort_name_default) + "：" + selectGroupName
                                }
                                break
                            }
                        }
                    }
                }
            }
            reflesh()
        }
    }

    /**
     * パスワード一覧応答通知リスナークラス
     *
     */
    inner class PasswordListCallbackListener : SimpleCallbackListener {
        /** 移動元位置 */
        private var fromPos = -1
        /** 移動先位置 */
        private var toPos = -1

        /**
         * 並べ替え中の移動処理
         *
         * @param viewHolder 移動元ビュー
         * @param target 移動先ビュー
         * @return
         */
        @Suppress("DEPRECATION")
        override fun onSimpleCallbackMove(
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            if (adapter?.editEnable == true && TextUtils.isEmpty(seachViewWord)) {
                // 移動元位置は最初のイベント時の値を保持する
                if (fromPos == -1) fromPos = viewHolder.adapterPosition
                // 通知用の移動元位置は毎回更新する
                val notifyFromPos = viewHolder.adapterPosition
                // 移動先位置は最後イベント時の値を保持する
                toPos = target.adapterPosition
                adapter?.notifyItemMoved(notifyFromPos, toPos)
                return true
            }
            return false
        }

        /**
         * 並べ替え完了後処理
         *
         * @param recyclerView ビュー全体
         * @param viewHolder 操作対象ビュー
         */
        override fun clearSimpleCallbackView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            // 入れ替え完了後に最後に一度DBの更新をする
            val rearrangePasswordList = adapter?.rearrangePasswordList(fromPos, toPos)
            rearrangePasswordList?.let { passwordListViewModel.update(rearrangePasswordList) }
            // 移動位置情報を初期化
            fromPos = -1
            toPos = -1
        }
    }

    companion object {
        /** データ編集要求コード */
        private const val EDIT_DATA = 1001
        /** 設定画面遷移要求コード */
        private const val START_SETTING = 1002
        /** グループ一覧画面遷移要求コード */
        private const val START_GROUP = 1003
    }
}