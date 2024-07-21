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
import kotlinx.coroutines.launch
import java.util.Arrays
import java.util.Date
import java.util.Locale

class PasswordListActivity : AppCompatActivity(), AdapterListener {
    private var selectGroupName: String? = null
    private var loginDataManager: LoginDataManager? = null
    // TODO:動作確認したらコメントアウトを削除
//    private var listDataManager: ListDataManager? = null
    private var simpleCallbackHelper: SimpleCallbackHelper? = null
    private var adContainerView: FrameLayout? = null
    private var mAdView: AdView? = null
    var recyclerView: RecyclerView? = null
    var adapter: PasswordListAdapter? = null
    private var menu: Menu? = null
    private var currentMenuSelect = 0
    private var currentMemoVisible: Boolean? = null
    var seachViewWord: String? = null

    private val passwordListViewModel: PasswordListViewModel by viewModels {
        PasswordListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_list)
        MobileAds.initialize(this) { }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                Arrays.asList(
                    getString(R.string.admob_test_device),
                    getString(R.string.admob_test_device_xaomi)
                )
            ).build()
        )
        adContainerView = findViewById(R.id.adView_frame)
        adContainerView?.post(Runnable { loadBanner() })
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
            ShowRateDialogCondition { appLaunchCount, appThisVersionCodeLaunchCount, firstLaunchDate, appVersionCode, previousAppVersionCode, rateClickDate, reminderClickDate, doNotShowAgain -> // レビュー依頼の文言を変えたバージョンでは、まだレビューをしておらず
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
        loginDataManager = LoginDataManager.Companion.getInstance(this)
//        listDataManager = ListDataManager.Companion.getInstance(this)

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager!!.displayBackgroundSwitchEnable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        currentMemoVisible = loginDataManager!!.memoVisibleSwitchEnable
        // TODO:動作確認したらコメントアウトを削除
//        listDataManager!!.setSelectGroupId(loginDataManager!!.selectGroup)
//        listDataManager!!.sortListData(loginDataManager!!.sortKey)
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
                                    .setPositiveButton(getString(R.string.delete_execute)) { dialog1: DialogInterface?, which: Int ->
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
                                    (holder as PasswordListAdapter.ViewHolder).title?.text.toString()
                                )
                                intent.putExtra(
                                    "ACCOUNT",
                                    (holder as PasswordListAdapter.ViewHolder).account
                                )
                                intent.putExtra(
                                    "PASSWORD",
                                    (holder as PasswordListAdapter.ViewHolder).password
                                )
                                intent.putExtra("URL", (holder as PasswordListAdapter.ViewHolder).url)
                                intent.putExtra("GROUP", (holder as PasswordListAdapter.ViewHolder).groupId)
                                intent.putExtra("MEMO", (holder as PasswordListAdapter.ViewHolder).memo)
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
                                // TODO:動作確認したらコメントアウトを削除
//                                intent.putExtra("ID", listDataManager?.newId)
                                intent.putExtra("EDIT", false)
                                intent.putExtra(
                                     "TITLE",
                                     (holder as PasswordListAdapter.ViewHolder).title?.text.toString() + " " + getString(
                                        R.string.copy_title
                                    )
                                )
                                intent.putExtra(
                                     "ACCOUNT",
                                    (holder as PasswordListAdapter.ViewHolder).account
                                )
                                intent.putExtra(
                                     "PASSWORD",
                                    (holder as PasswordListAdapter.ViewHolder).password
                                )
                                intent.putExtra("URL", (holder as PasswordListAdapter.ViewHolder).url)
                                intent.putExtra("GROUP", (holder as PasswordListAdapter.ViewHolder).groupId)
                                intent.putExtra("MEMO", (holder as PasswordListAdapter.ViewHolder).memo)
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
            // TODO:動作確認したらコメントアウトを削除
//            intent.putExtra("ID", listDataManager?.newId)
            intent.putExtra("EDIT", false)
            startActivityForResult(intent, EDIT_DATA)
        }

        // 渡されたデータを取得する
        val intent = intent
        val first_time = intent.getBooleanExtra("FIRST_TIME", false)
        if (first_time) {
            intent.putExtra("FIRST_TIME", false)
            operationInstructionDialog()
        }
    }

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

    private val adSize: AdSize
        private get() {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                // 編集状態は解除する
                adapter?.editEnable = false
                finish()
            }

            R.id.edit_mode -> {
                // 編集状態の変更
                // TODO:動作確認したらコメントアウトを削除
//                listDataManager!!.sortListData(ListDataManager.Companion.SORT_ID)
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

            R.id.sort_default -> {
                setCurrentSelectMenuTitle(item, R.id.sort_default)
                title = getString(R.string.sort_name_default) + "：" + selectGroupName
//                listDataManager!!.sortListData(ListDataManager.Companion.SORT_ID)
                adapter?.sortPasswordList(PasswordListAdapter.SORT_ID)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_ID)
            }

            R.id.sort_title -> {
                setCurrentSelectMenuTitle(item, R.id.sort_title)
                title = getString(R.string.sort_name_title) + "：" + selectGroupName
//                listDataManager!!.sortListData(ListDataManager.Companion.SORT_TITLE)
                adapter?.sortPasswordList(PasswordListAdapter.SORT_TITLE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_TITLE)
            }

            R.id.sort_update -> {
                setCurrentSelectMenuTitle(item, R.id.sort_update)
                title = getString(R.string.sort_name_update) + "：" + selectGroupName
//                listDataManager!!.sortListData(ListDataManager.Companion.SORT_INPUTDATE)
                adapter?.sortPasswordList(PasswordListAdapter.SORT_INPUTDATE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager!!.setSortKey(PasswordListAdapter.SORT_INPUTDATE)
            }

            R.id.select_group -> {
                // 設定画面へ遷移
                val intentGroup = Intent(this@PasswordListActivity, GroupListActivity::class.java)
                startActivityForResult(intentGroup, START_GROUP)
            }

            R.id.setting_menu -> {
                // 設定画面へ遷移
                val intent = Intent(this@PasswordListActivity, SettingActivity::class.java)
                startActivityForResult(intent, START_SETTING)
            }

            else -> {}
        }
        return super.onOptionsItemSelected(item)
    }

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
        // TODO:動作確認したらコメントアウトを削除
//        for (group in listDataManager!!.groupList) {
//            if (java.lang.Long.valueOf(group!!["group_id"]) == loginDataManager!!.selectGroup) {
//                selectGroupName = group["name"]
//                listDataManager!!.setSelectGroupId(loginDataManager!!.selectGroup)
//                isSelectGroupExist = true
//                break
//            }
//        }
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
                    // TODO:動作確認したらコメントアウトを削除
//                    listDataManager!!.setSelectGroupId(1L)
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

    // 入力文字列に応じてフィルタをする
    fun setSearchWordFilter() {
        val filter = (recyclerView!!.adapter as Filterable?)!!.filter
        if (TextUtils.isEmpty(seachViewWord)) {
            filter.filter(null)
        } else {
            filter.filter(seachViewWord!!.lowercase(Locale.getDefault()))
        }
    }

    // データの一覧を更新する
    fun reflesh() {
        adapter!!.notifyDataSetChanged()
        // フィルタしている場合はフィルタデータの一覧も更新する
        setSearchWordFilter()
    }

    public override fun onDestroy() {
        // TODO:動作確認をしたらコメントアウトを削除
//        listDataManager!!.closeData()
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager!!.displayBackgroundSwitchEnable && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_DATA || requestCode == START_GROUP || requestCode == START_SETTING && resultCode == SettingActivity.Companion.NEED_UPDATE) {
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


    inner class PasswordListCallbackListener : SimpleCallbackListener {
        private var fromPos = -1
        private var toPos = -1
        override fun onSimpleCallbackMove(
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            if (adapter?.editEnable == true && TextUtils.isEmpty(seachViewWord)) {
                // TODO:動作に問題が無いことが確認できたら消す
//                val fromPos = viewHolder.adapterPosition
//                val toPos = target.adapterPosition
//                adapter!!.notifyItemMoved(fromPos, toPos)
//                listDataManager!!.rearrangeData(fromPos, toPos)
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

        override fun clearSimpleCallbackView(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) {
            // TODO:動作に問題が無いことが確認できたら消す
//            recyclerView.adapter = adapter
            // 入れ替え完了後に最後に一度DBの更新をする
            val rearrangePasswordList = adapter?.rearrangePasswordList(fromPos, toPos)
            rearrangePasswordList?.let { passwordListViewModel.update(rearrangePasswordList) }
            // 移動位置情報を初期化
            fromPos = -1
            toPos = -1
        }
    }

    companion object {
        private const val EDIT_DATA = 1001
        private const val START_SETTING = 1002
        private const val START_GROUP = 1003
    }
}