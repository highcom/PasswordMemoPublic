package com.highcom.passwordmemo.ui.fragment

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Filterable
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.databinding.AlertOperatingInstructionsBinding
import com.highcom.passwordmemo.databinding.FragmentPasswordListBinding
import com.highcom.passwordmemo.ui.DividerItemDecoration
import com.highcom.passwordmemo.ui.PasswordEditData
import com.highcom.passwordmemo.ui.list.PasswordListAdapter
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import com.highcom.passwordmemo.domain.AdBanner
import com.highcom.passwordmemo.domain.login.LoginDataManager
import com.highcom.passwordmemo.PasswordMemoDrawerActivity
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.ui.list.DrawerGroupListAdapter
import dagger.hilt.android.AndroidEntryPoint
import jp.co.recruit_mp.android.rmp_appirater.RmpAppirater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * パスワード一覧画面フラグメント
 *
 */
@AndroidEntryPoint
class PasswordListFragment : Fragment(), PasswordListAdapter.AdapterListener {
    /** パスワード一覧画面のバインディング */
    private lateinit var binding: FragmentPasswordListBinding
    /** 選択グループ名称 */
    private var selectGroupName: String? = null
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** スワイプボタン表示用通知ヘルパー */
    private var simpleCallbackHelper: SimpleCallbackHelper? = null
    /** バナー広告処理 */
    private var adBanner: AdBanner? = null
    /** 広告コンテナ */
    private var adContainerView: FrameLayout? = null
    /** パスワード一覧表示用リサイクラービュー */
    var recyclerView: RecyclerView? = null
    /** パスワード一覧用アダプタ */
    var adapter: PasswordListAdapter? = null
    /** ドロワーのグループ一覧用アダプタ */
    private var drawerAdapter: DrawerGroupListAdapter? = null
    /** 操作メニュー */
    private var menu: Menu? = null
    /** 現在洗濯中のメニュー */
    private var currentMenuSelect = 0
    /** 現在のメモ表示設定 */
    private var currentMemoVisible: Boolean? = null
    /** 検索文字列 */
    var seachViewWord: String? = null
    /** パスワード一覧ビューモデル */
    private val passwordListViewModel: PasswordListViewModel by hiltNavGraphViewModels(R.id.passwordmemo_nav_graph)
    /** グループ一覧ビューモデル */
    private val groupListViewModel: GroupListViewModel by viewModels()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fragmentのメニューを有効にする
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_password_list, container, false)
        binding.passwordListViewModel = passwordListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainerView = binding.adViewFrame
        adBanner = AdBanner(this, adContainerView)
        adContainerView?.post { adBanner?.loadBanner(getString(R.string.admob_unit_id_1)) }
        // ドロワーを操作可能にする
        val activity = requireActivity()
        if (activity is PasswordMemoDrawerActivity) {
            activity.drawerMenuEnabled()
            activity.logoutButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, loginDataManager.textSize)
            activity.logoutButton.setOnClickListener {
                // 編集状態は解除する
                adapter?.editEnable = false
                findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToLoginFragment())
            }
        }

        // レビュー評価依頼のダイアログに表示する内容を設定
        val options = RmpAppirater.Options(
            getString(R.string.review_dialig_title),
            getString(R.string.review_dialig_message),
            getString(R.string.review_dialig_rate),
            getString(R.string.review_dialig_rate_later),
            getString(R.string.review_dialig_rate_cancel)
        )
        RmpAppirater.appLaunched(
            context,
            RmpAppirater.ShowRateDialogCondition { appLaunchCount, appThisVersionCodeLaunchCount, _, appVersionCode, _, rateClickDate, reminderClickDate, doNotShowAgain ->
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

        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        currentMemoVisible = loginDataManager.memoVisibleSwitchEnable
        adapter = PasswordListAdapter(
            requireContext(),
            viewLifecycleOwner,
            loginDataManager,
            this
        )
        adapter?.textSize = loginDataManager.textSize
        recyclerView = binding.passwordListView
        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter

        // 選択されているグループのパスワード一覧を設定する
        passwordListViewModel.setSelectGroup(loginDataManager.selectGroup)
        @Suppress("DEPRECATION")
        lifecycleScope.launchWhenStarted {
            passwordListViewModel.passwordList.collect { list ->
                adapter?.setList(list)
                adapter?.sortPasswordList(loginDataManager.sortKey)
                reflesh()

                // データ更新後にスクロール位置を復元
                passwordListViewModel.recyclerViewState?.let { state ->
                    recyclerView?.layoutManager?.onRestoreInstanceState(state)
                    passwordListViewModel.recyclerViewState = null // 一度復元したらクリア
                }
            }
        }

        // セル間に区切り線を実装する
        val itemDecoration: RecyclerView.ItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL_LIST)
        recyclerView!!.addItemDecoration(itemDecoration)
        val scale = resources.displayMetrics.density
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper =
            object : SimpleCallbackHelper(context, recyclerView, scale, PasswordListCallbackListener()) {
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
                        viewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                context?.let {
                                    AlertDialog.Builder(it)
                                        .setTitle(
                                            getString(R.string.delete_title_head) + (holder as PasswordListAdapter.RowPasswordViewHolder).binding.passwordEntity?.title + getString(
                                                R.string.delete_title
                                            )
                                        )
                                        .setMessage(getString(R.string.delete_message))
                                        .setPositiveButton(getString(R.string.delete_execute)) { _: DialogInterface?, _: Int ->
                                            holder.binding.passwordEntity?.id?.let { id -> passwordListViewModel.delete(id) }
                                            simpleCallbackHelper!!.resetSwipePos()
                                            reflesh()
                                        }
                                        .setNegativeButton(getString(R.string.delete_cancel), null)
                                        .show()
                                }
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
                                // 選択アイテムを編集モードで設定
                                val entity = (holder as PasswordListAdapter.RowPasswordViewHolder).binding.passwordEntity
                                val passwordEditData = PasswordEditData(
                                    edit = true,
                                    id = entity?.id ?: 0,
                                    title = entity?.title ?: "",
                                    account = entity?.account ?: "",
                                    password =  entity?.password ?: "",
                                    url = entity?.url ?: "",
                                    groupId = entity?.groupId ?: 1,
                                    memo = entity?.memo ?: "",
                                    color = entity?.color ?: 0
                                )
                                findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToInputPasswordFragment(editData = passwordEditData))
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
                                // 選択アイテムを複製モードで設定
                                val entity = (holder as PasswordListAdapter.RowPasswordViewHolder).binding.passwordEntity
                                val passwordEditData = PasswordEditData(
                                    edit = false,
                                    title = entity?.title + " " + getString(R.string.copy_title),
                                    account = entity?.account ?: "",
                                    password =  entity?.password ?: "",
                                    url = entity?.url ?: "",
                                    groupId = entity?.groupId ?: 1,
                                    memo = entity?.memo ?: "",
                                    color = entity?.color ?: 0
                                )
                                findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToInputPasswordFragment(editData = passwordEditData))
                            }
                        }
                    ))
                }
            }

        // フローティングボタンからの新規追加処理
        binding.fab.setOnClickListener {
            val passwordEditData = PasswordEditData()
            // 選択されているグループIDを設定
            passwordEditData.groupId = loginDataManager.selectGroup
            findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToInputPasswordFragment(editData = passwordEditData))
        }

        // 渡されたデータを取得する
        val args: PasswordListFragmentArgs by navArgs()
        if (args.firstTime) {
            operationInstructionDialog()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_list, menu)
        this.menu = menu
        currentMenuSelect = when (loginDataManager.sortKey) {
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
            searchView!!.findViewById<SearchView.SearchAutoComplete>(androidx.appcompat.R.id.search_src_text)
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
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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
                requireActivity().title = getString(R.string.sort_name_default) + "：" + selectGroupName
                recyclerView!!.adapter = adapter
                loginDataManager.setSortKey(PasswordListAdapter.SORT_ID)
            }
            // 標準ソート
            R.id.sort_default -> {
                setCurrentSelectMenuTitle(item, R.id.sort_default)
                requireActivity().title = getString(R.string.sort_name_default) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_ID)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager.setSortKey(PasswordListAdapter.SORT_ID)
            }
            // タイトルソート
            R.id.sort_title -> {
                setCurrentSelectMenuTitle(item, R.id.sort_title)
                requireActivity().title = getString(R.string.sort_name_title) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_TITLE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager.setSortKey(PasswordListAdapter.SORT_TITLE)
            }
            // 更新日ソート
            R.id.sort_update -> {
                setCurrentSelectMenuTitle(item, R.id.sort_update)
                requireActivity().title = getString(R.string.sort_name_update) + "：" + selectGroupName
                adapter?.sortPasswordList(PasswordListAdapter.SORT_INPUTDATE)
                if (adapter?.editEnable == true) adapter?.editEnable = false
                recyclerView!!.adapter = adapter
                loginDataManager.setSortKey(PasswordListAdapter.SORT_INPUTDATE)
            }
            // グループ選択
            R.id.select_group -> {
                // グループ一覧画面へ遷移
                findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToGroupListFragment())
            }
            // 設定メニュー
            R.id.setting_menu -> {
                // 設定画面へ遷移
                findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToSettingFragment())
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

    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        // 背景色を設定する
        binding.passwordListFragmentView.setBackgroundColor(loginDataManager.backgroundColor)
        selectGroupName = getString(R.string.list_title)
        var isSelectGroupExist = false
        lifecycleScope.launchWhenStarted {
            val initGroupList = groupListViewModel.groupList.first()
            // グループデータがない場合はデフォルトデータとして「すべて」を必ず追加」
            if (initGroupList.isEmpty()) {
                groupListViewModel.insert(GroupEntity(1, 1, getString(R.string.list_title), 0))
            }
            // グループデータから各種設定
            groupListViewModel.groupList.collect { list ->
                // 選択グループ名の設定
                for (entity in list) {
                    if (entity.groupId == loginDataManager.selectGroup) {
                        selectGroupName = entity.name
                        isSelectGroupExist = true
                    }
                }

                // ドロワーのグループ一覧を設定
                drawerAdapter = DrawerGroupListAdapter(requireContext(), list)
                drawerAdapter?.textSize = loginDataManager.textSize
                val drawerActivity = requireActivity()
                if (drawerActivity is PasswordMemoDrawerActivity) {
                    drawerActivity.drawerGroupList.adapter = drawerAdapter
                    drawerActivity.drawerGroupList.setOnItemClickListener { _, _, position, _ ->
                        // 選択されたグループを設定
                        selectGroupName = list[position].name
                        isSelectGroupExist = true
                        loginDataManager.setSelectGroup(list[position].groupId)
                        passwordListViewModel.setSelectGroup(loginDataManager.selectGroup)
                        // タイトルに選択しているグループ名を設定
                        requireActivity().title = when (loginDataManager.sortKey) {
                            PasswordListAdapter.SORT_ID -> getString(R.string.sort_name_default) + "：" + selectGroupName
                            PasswordListAdapter.SORT_TITLE -> getString(R.string.sort_name_title) + "：" + selectGroupName
                            PasswordListAdapter.SORT_INPUTDATE -> getString(R.string.sort_name_update) + "：" + selectGroupName
                            else -> getString(R.string.sort_name_default) + "：" + selectGroupName
                        }
                        drawerActivity.drawerLayout.closeDrawers()
                    }
                }

                // 選択していたグループが存在しなくなった場合には「すべて」にリセットする
                if (!isSelectGroupExist) {
                    loginDataManager.setSelectGroup(1L)
                    passwordListViewModel.setSelectGroup(1L)
                }
                // タイトルに選択しているグループ名を設定
                requireActivity().title = when (loginDataManager.sortKey) {
                    PasswordListAdapter.SORT_ID -> getString(R.string.sort_name_default) + "：" + selectGroupName
                    PasswordListAdapter.SORT_TITLE -> getString(R.string.sort_name_title) + "：" + selectGroupName
                    PasswordListAdapter.SORT_INPUTDATE -> getString(R.string.sort_name_update) + "：" + selectGroupName
                    else -> getString(R.string.sort_name_default) + "：" + selectGroupName
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // スクロール位置を保存
        passwordListViewModel.recyclerViewState = recyclerView?.layoutManager?.onSaveInstanceState()
    }

    override fun onResume() {
        super.onResume()
        var needRefresh = false
        // メモ表示設定に変更があった場合には再描画する
        if (currentMemoVisible != loginDataManager.memoVisibleSwitchEnable) {
            currentMemoVisible = loginDataManager.memoVisibleSwitchEnable
            needRefresh = true
        }
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter?.textSize != loginDataManager.textSize) {
            adapter?.textSize = loginDataManager.textSize
            needRefresh = true
        }
        // テキストサイズ設定に変更があった場合にはドロワーも再描画する
        if (drawerAdapter?.textSize != loginDataManager.textSize) {
            drawerAdapter?.textSize = loginDataManager.textSize
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
        val alertBinding = AlertOperatingInstructionsBinding.inflate(layoutInflater)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.operation_opening_title)
            .setView(alertBinding.root)
            .setPositiveButton(R.string.close, null)
            .create()
        alertDialog.show()
        alertBinding.operationInstructionMessage.visibility = View.VISIBLE
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
        adapter?.notifyDataSetChanged()
        drawerAdapter?.notifyDataSetChanged()
        // フィルタしている場合はフィルタデータの一覧も更新する
        setSearchWordFilter()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        adBanner?.destroy()
    }

    /**
     * パスワード一覧の項目タップ時の処理
     *
     * @param passwordEntity 選択対象パスワードデータ
     */
    override fun onAdapterClicked(passwordEntity: PasswordEntity) {
        // 編集状態の場合は入力画面に遷移しない
        if (adapter?.editEnable == true) {
            return
        }
        // 選択アイテムを設定
        val passwordEditData = PasswordEditData(
            id = passwordEntity.id,
            title = passwordEntity.title,
            account = passwordEntity.account,
            password =  passwordEntity.password,
            url = passwordEntity.url,
            groupId = passwordEntity.groupId,
            memo = passwordEntity.memo,
            color = passwordEntity.color
        )
        // 入力画面に遷移
        findNavController().navigate(PasswordListFragmentDirections.actionPasswordListFragmentToReferencePasswordFragment(editData = passwordEditData))
    }

    /**
     * パスワード一覧の項目変更時の処理
     *
     * @param passwordEntity 変更対象パスワードデータ
     */
    override fun onAdapterChanged(passwordEntity: PasswordEntity) {
        passwordListViewModel.update(passwordEntity)
    }

    /**
     * パスワード一覧応答通知リスナークラス
     *
     */
    inner class PasswordListCallbackListener : SimpleCallbackHelper.SimpleCallbackListener {
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
}