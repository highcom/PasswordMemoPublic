package com.highcom.passwordmemo

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.ui.DividerItemDecoration
import com.highcom.passwordmemo.ui.list.GroupListAdapter
import com.highcom.passwordmemo.ui.list.GroupListAdapter.GroupAdapterListener
import com.highcom.passwordmemo.ui.list.GroupListAdapter.GroupViewHolder
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper.SimpleCallbackListener
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * グループ一覧画面アクティビティ
 *
 */
class GroupListActivity : AppCompatActivity(), GroupAdapterListener {
    /** ログインデータ管理 */
    private var loginDataManager: LoginDataManager? = null
    /** 広告用コンテナ */
    private var adContainerView: FrameLayout? = null
    /** 広告ビュー */
    private var mAdView: AdView? = null
    /** グループ一覧用リサイクラービュー */
    var recyclerView: RecyclerView? = null
    /** グループ追加フローティングボタン */
    private var groupFab: FloatingActionButton? = null
    /** グループ一覧用アダプタ */
    var adapter: GroupListAdapter? = null
    /** スワイプボタン表示用通知ヘルパー */
    private var simpleCallbackHelper: SimpleCallbackHelper? = null
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels {
        GroupListViewModel.Factory((application as PasswordMemoApplication).repository)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_list)
        loginDataManager = LoginDataManager.getInstance(application)
        MobileAds.initialize(this) { }
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder().setTestDeviceIds(
                /* testDeviceIds = */ mutableListOf(
                    "874848BA4D9A6B9B0A256F7862A47A31",
                    "A02A04D245766C519D07D09F0E258E1E"
                )
            ).build()
        )
        adContainerView = findViewById(R.id.adView_groupFrame)
        adContainerView?.post { loadBanner() }
        title = getString(R.string.group_title) + getString(R.string.group_title_select)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        loginDataManager = LoginDataManager.getInstance(application)
        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager?.displayBackgroundSwitchEnable == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        adapter = GroupListAdapter(
            this,
            this
        )
        adapter?.textSize = loginDataManager!!.textSize
        recyclerView = findViewById<View>(R.id.groupListView) as RecyclerView
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter

        lifecycleScope.launch {
            val initGroupList = groupListViewModel.groupList.first()
            // グループ名が空白のデータが存在していた場合には削除する
            for (group in initGroupList) {
                if (group.name.isEmpty()) {
                    groupListViewModel.delete(group.groupId)
                    groupListViewModel.resetGroupId(group.groupId)
                }
                // 削除するデータが選択されていた場合には「すべて」にリセットする
                if (loginDataManager?.selectGroup == group.groupId) {
                    loginDataManager?.setSelectGroup(1L)
                }
            }
            // グループリストの監視をする
            groupListViewModel.groupList.collect { list ->
                // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
                if (list.isEmpty()) {
                    groupListViewModel.insert(GroupEntity(1, 1, getString(R.string.list_title)))
                }
                adapter?.groupList = list
                adapter?.notifyDataSetChanged()
            }
        }

        // セル間に区切り線を実装する
        val itemDecoration: ItemDecoration =
            DividerItemDecoration(this, DividerItemDecoration.Companion.VERTICAL_LIST)
        recyclerView!!.addItemDecoration(itemDecoration)
        groupFab = findViewById(R.id.groupFab)
        groupFab?.setOnClickListener {
            groupListViewModel.insert(GroupEntity(0, (adapter?.groupList?.size ?: 0) + 1, ""))
            lifecycleScope.launch {
                groupListViewModel.groupList.collect { list ->
                    adapter?.notifyDataSetChanged()
                    // 新規作成時は対象のセルにフォーカスされるようにスクロールする
                    for (position in list.indices) {
                        if (list[position].name == "") {
                            recyclerView!!.smoothScrollToPosition(position)
                            break
                        }
                    }
                }
            }
        }
        if (adapter?.editEnable == false) groupFab?.visibility = View.GONE
        val scale = resources.displayMetrics.density
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper =
            object : SimpleCallbackHelper(applicationContext, recyclerView, scale, GroupListCallbackListener()) {
                @SuppressLint("ResourceType")
                override fun instantiateUnderlayButton(
                    viewHolder: RecyclerView.ViewHolder,
                    underlayButtons: MutableList<UnderlayButton>
                ) {
                    if (viewHolder.itemView.id == R.id.row_footer) return
                    // グループIDの1はすべてなので削除出来ないようにする
                    if ((viewHolder as GroupViewHolder).groupId == 1L) return
                    underlayButtons.add(UnderlayButton(
                        getString(R.string.delete),
                        BitmapFactory.decodeResource(resources, R.drawable.ic_delete),
                        Color.parseColor(getString(R.color.underlay_red)),
                        viewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                AlertDialog.Builder(this@GroupListActivity)
                                    .setTitle(
                                        getString(R.string.delete_title_head) + (holder as GroupViewHolder).groupName?.text.toString() + getString(
                                            R.string.delete_title
                                        )
                                    )
                                    .setMessage(getString(R.string.delete_message))
                                    .setPositiveButton(getString(R.string.delete_execute)) { _: DialogInterface?, _: Int ->
                                        holder.groupId?.let {
                                            groupListViewModel.delete(it)
                                            groupListViewModel.resetGroupId(it)
                                        }
                                        if (loginDataManager?.selectGroup == holder.groupId) {
                                            loginDataManager?.setSelectGroup(1L)
                                        }
                                        simpleCallbackHelper?.resetSwipePos()
                                        adapter!!.notifyDataSetChanged()
                                    }
                                    .setNegativeButton(getString(R.string.delete_cancel), null)
                                    .show()

                            }
                        }
                    ))
                }
            }
    }

    /**
     * バナー広告ロード処理
     *
     */
    private fun loadBanner() {
        // Create an ad request.
        mAdView = AdView(this)
        mAdView!!.adUnitId = getString(R.string.admob_unit_id_4)
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

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        // 背景色を設定する
        (findViewById<View>(R.id.groupListActivityView) as ConstraintLayout).setBackgroundColor(
            loginDataManager!!.backgroundColor
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        var needRefresh = false
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter?.textSize != loginDataManager?.textSize) {
            adapter?.textSize = loginDataManager!!.textSize
            needRefresh = true
        }
        if (needRefresh) adapter!!.notifyDataSetChanged()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_group_mode, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                recyclerView!!.adapter = adapter
                finish()
            }

            R.id.select_group_mode -> {
                if (adapter?.editEnable == true) {
                    adapter?.editEnable = false
                    title = getString(R.string.group_title) + getString(R.string.group_title_select)
                    groupFab!!.visibility = View.GONE
                } else {
                    adapter?.editEnable = true
                    title = getString(R.string.group_title) + getString(R.string.group_title_edit)
                    groupFab!!.visibility = View.VISIBLE
                }
                recyclerView!!.adapter = adapter
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        runBlocking {
            val groupList = groupListViewModel.groupList.first()
            // グループ名が空白のデータが存在していた場合には削除する
            for (group in groupList) {
                if (group.name.isEmpty()) {
                    groupListViewModel.delete(group.groupId)
                    groupListViewModel.resetGroupId(group.groupId)
                }
            }
        }
        super.onStop()
    }
    public override fun onDestroy() {
        if (mAdView != null) mAdView!!.destroy()
        //バックグラウンドの場合、全てのActivityを破棄してログイン画面に戻る
        if (loginDataManager?.displayBackgroundSwitchEnable == true && PasswordMemoLifecycle.Companion.isBackground) {
            finishAffinity()
        }
        super.onDestroy()
    }

    override fun onGroupNameClicked(view: View, groupId: Long?) {
        if (adapter?.editEnable == true) {
            view.post {
                groupFab!!.visibility = View.GONE
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                view.requestFocus()
                val inputMethodManager =
                    getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(view, 0)
            }
        } else {
            loginDataManager?.setSelectGroup(groupId!!)
            finish()
        }
    }

    override fun onGroupNameOutOfFocused(view: View, groupEntity: GroupEntity) {
        // 内容編集中にフォーカスが外れた場合は、キーボードを閉じる
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        if (adapter?.editEnable == true) {
            groupFab!!.visibility = View.VISIBLE
        }
        view.isFocusable = false
        view.isFocusableInTouchMode = false
        view.requestFocus()
        // 内容が空白の場合には削除する
        if (groupEntity.name.isEmpty()) {
            groupListViewModel.delete(groupEntity.groupId)
            groupListViewModel.resetGroupId(groupEntity.groupId)
            if (loginDataManager?.selectGroup == groupEntity.groupId) {
                loginDataManager?.setSelectGroup(1L)
            }
            return
        }
        groupListViewModel.update(groupEntity)
    }

    /**
     * グループ一覧通知用リスナークラス
     *
     */
    inner class GroupListCallbackListener : SimpleCallbackListener {
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
            if (adapter?.editEnable == false) return false
            // グループ名が入力されていない場合は移動させない
            if ((viewHolder as GroupViewHolder).groupName?.text.toString() == "" || (target as GroupViewHolder).groupName?.text.toString() == "") return false
            // グループ名が入力途中でDB反映されていないデータも並び替えさせない
            adapter?.groupList?.let {
                for (entity in it) {
                    if (entity.groupId == viewHolder.groupId || entity.groupId == target.groupId) {
                        if (entity.name.isEmpty()) return false
                    }
                }
            }
            // 移動元位置は最初のイベント時の値を保持する
            if (fromPos == -1) fromPos = viewHolder.adapterPosition
            // 通知用の移動元位置は毎回更新する
            val notifyFromPos = viewHolder.adapterPosition
            // 移動先位置は最後イベント時の値を保持する
            toPos = target.adapterPosition
            // 1番目のデータは「すべて」なので並べ替え不可にする
            if (fromPos == 0 || toPos == 0) return false
            adapter?.notifyItemMoved(notifyFromPos, toPos)
            return true
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
            val rearrangeList = adapter?.rearrangeGroupList(fromPos, toPos)
            rearrangeList?.let { groupListViewModel.update(rearrangeList) }
            // 移動位置情報を初期化
            fromPos = -1
            toPos = -1
        }
    }
}