package com.highcom.passwordmemo.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.highcom.passwordmemo.R
import android.annotation.SuppressLint
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.graphics.Color
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.databinding.FragmentGroupListBinding
import com.highcom.passwordmemo.ui.DividerItemDecoration
import com.highcom.passwordmemo.ui.list.GroupListAdapter
import com.highcom.passwordmemo.ui.list.GroupListAdapter.RowGroupViewHolder
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper
import com.highcom.passwordmemo.ui.list.SimpleCallbackHelper.SimpleCallbackListener
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import com.highcom.passwordmemo.domain.AdBanner
import com.highcom.passwordmemo.domain.login.LoginDataManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * グループ一覧画面フラグメント
 *
 */
@AndroidEntryPoint
class GroupListFragment : Fragment(), GroupListAdapter.GroupAdapterListener {
    /** グループ一覧画面のバインディング */
    private lateinit var binding: FragmentGroupListBinding
    /** ログインデータ管理 */
    @Inject
    lateinit var loginDataManager: LoginDataManager
    /** バナー広告処理 */
    private var adBanner: AdBanner? = null
    /** 広告用コンテナ */
    private var adContainerView: FrameLayout? = null
    /** グループ一覧用リサイクラービュー */
    var recyclerView: RecyclerView? = null
    /** グループ追加フローティングボタン */
    private var groupFab: FloatingActionButton? = null
    /** グループ一覧用アダプタ */
    var adapter: GroupListAdapter? = null
    /** スワイプボタン表示用通知ヘルパー */
    private var simpleCallbackHelper: SimpleCallbackHelper? = null
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
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_list, container, false)
        binding.groupListViewModel = groupListViewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adContainerView = binding.adViewGroupFrame
        adBanner = AdBanner(this, adContainerView)
        adContainerView?.post { adBanner?.loadBanner(getString(R.string.admob_unit_id_4)) }
        requireActivity().title = getString(R.string.group_title) + getString(R.string.group_title_select)
        // ActionBarに戻るボタンを設定
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // バックグラウンドでは画面の中身が見えないようにする
        if (loginDataManager.displayBackgroundSwitchEnable) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        adapter = GroupListAdapter(
            requireContext(),
            viewLifecycleOwner,
            this
        )
        adapter?.textSize = loginDataManager.textSize
        recyclerView = binding.groupListView
        recyclerView!!.layoutManager = LinearLayoutManager(requireContext())
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
                if (loginDataManager.selectGroup == group.groupId) {
                    loginDataManager.setSelectGroup(1L)
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
                // 新規作成時は対象のセルにフォーカスされるようにスクロールする
                for (position in list.indices) {
                    if (list[position].name == "") {
                        recyclerView!!.scrollToPosition(position)
                        break
                    }
                }
            }
        }

        // セル間に区切り線を実装する
        val itemDecoration: ItemDecoration =
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL_LIST)
        recyclerView!!.addItemDecoration(itemDecoration)
        groupFab = binding.groupFab
        groupFab?.setOnClickListener {
            groupListViewModel.insert(GroupEntity(0, (adapter?.groupList?.size ?: 0) + 1, ""))
        }
        if (adapter?.editEnable == false) groupFab?.visibility = View.GONE
        val scale = resources.displayMetrics.density
        // ドラックアンドドロップの操作を実装する
        simpleCallbackHelper =
            object : SimpleCallbackHelper(requireContext(), recyclerView, scale, GroupListCallbackListener()) {
                @SuppressLint("ResourceType")
                override fun instantiateUnderlayButton(
                    viewHolder: RecyclerView.ViewHolder,
                    underlayButtons: MutableList<UnderlayButton>
                ) {
                    if (viewHolder.itemView.id == R.id.row_footer) return
                    // グループIDの1はすべてなので削除出来ないようにする
                    if ((viewHolder as RowGroupViewHolder).binding.groupEntity?.groupId == 1L) return
                    underlayButtons.add(UnderlayButton(
                        getString(R.string.delete),
                        BitmapFactory.decodeResource(resources, R.drawable.ic_delete),
                        Color.parseColor(getString(R.color.underlay_red)),
                        viewHolder,
                        object : UnderlayButtonClickListener {
                            override fun onClick(holder: RecyclerView.ViewHolder, pos: Int) {
                                AlertDialog.Builder(requireContext())
                                    .setTitle(
                                        getString(R.string.delete_title_head) + (holder as RowGroupViewHolder).binding.groupEntity?.name + getString(
                                            R.string.delete_title
                                        )
                                    )
                                    .setMessage(getString(R.string.delete_message))
                                    .setPositiveButton(getString(R.string.delete_execute)) { _: DialogInterface?, _: Int ->
                                        holder.binding.groupEntity?.groupId?.let {
                                            groupListViewModel.delete(it)
                                            groupListViewModel.resetGroupId(it)
                                        }
                                        if (loginDataManager.selectGroup == holder.binding.groupEntity?.groupId) {
                                            loginDataManager.setSelectGroup(1L)
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

    @SuppressLint("ResourceType")
    override fun onStart() {
        super.onStart()
        // 背景色を設定する
        binding.groupListFragmentView.setBackgroundColor(
            loginDataManager.backgroundColor
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        var needRefresh = false
        // テキストサイズ設定に変更があった場合には再描画する
        if (adapter?.textSize != loginDataManager.textSize) {
            adapter?.textSize = loginDataManager.textSize
            needRefresh = true
        }
        if (needRefresh) adapter!!.notifyDataSetChanged()
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "inflater.inflate(R.menu.menu_group_mode, menu)",
        "com.highcom.passwordmemo.R"
    )
    )
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_group_mode, menu)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                recyclerView!!.adapter = adapter
                findNavController().navigate(GroupListFragmentDirections.actionGroupListFragmentToPasswordListFragment())
            }

            R.id.select_group_mode -> {
                if (adapter?.editEnable == true) {
                    adapter?.editEnable = false
                    requireActivity().title = getString(R.string.group_title) + getString(R.string.group_title_select)
                    groupFab!!.visibility = View.GONE
                } else {
                    adapter?.editEnable = true
                    requireActivity().title = getString(R.string.group_title) + getString(R.string.group_title_edit)
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

    override fun onDestroyView() {
        super.onDestroyView()
        adBanner?.destroy()
    }

    /**
     * グループ名称選択イベント
     *
     * @param view 選択対象ビュー
     * @param groupId グループID
     */
    override fun onGroupNameClicked(view: View, groupId: Long?) {
        if (adapter?.editEnable == true) {
            view.post {
                groupFab!!.visibility = View.GONE
                view.isFocusable = true
                view.isFocusableInTouchMode = true
                view.requestFocus()
                // Navigationでバックされた後にイベントが発生するためactivityがnullでない場合のみ実施
                val inputMethodManager =
                    activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
                inputMethodManager?.showSoftInput(view, 0)
            }
        } else {
            loginDataManager.setSelectGroup(groupId!!)
            findNavController().navigate(GroupListFragmentDirections.actionGroupListFragmentToPasswordListFragment())
        }
    }

    /**
     * グループ名称からフォーカスが外れたイベント
     *
     * @param view 選択対象ビュー
     * @param groupEntity グループデータ
     */
    override fun onGroupNameOutOfFocused(view: View, groupEntity: GroupEntity) {
        // 内容編集中にフォーカスが外れた場合は、キーボードを閉じる
        // Navigationでバックされた後にイベントが発生するためactivityがnullでない場合のみ実施
        val inputMethodManager = activity?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(
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
            if (loginDataManager.selectGroup == groupEntity.groupId) {
                loginDataManager.setSelectGroup(1L)
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
            if ((viewHolder as RowGroupViewHolder).binding.groupEntity?.name == "" || (target as RowGroupViewHolder).binding.groupEntity?.name == "") return false
            // グループ名が入力途中でDB反映されていないデータも並び替えさせない
            adapter?.groupList?.let {
                for (entity in it) {
                    if (entity.groupId == viewHolder.binding.groupEntity?.groupId || entity.groupId == target.binding.groupEntity?.groupId) {
                        if (entity.name.isEmpty()) return false
                    }
                }
            }
            // 移動元位置は最初のイベント時の値を保持する
            if (fromPos == -1) fromPos = viewHolder.adapterPosition
            // 通知用の移動元位置は毎回更新する
            val notifyFromPos = viewHolder.adapterPosition
            // 通知用の移動先位置は毎回更新する
            val notifyToPos = target.adapterPosition
            // 1番目のデータは「すべて」なので並べ替え不可にする
            if (fromPos == 0 || notifyToPos == 0) return true
            // 移動先位置は最後イベント時の値を保持する
            toPos = target.adapterPosition
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
            // 1番目のデータは「すべて」なので並べ替え不可にする
            if (fromPos == -1 || toPos == -1 || fromPos == 0 || toPos == 0) {
                // 移動位置情報を初期化
                fromPos = -1
                toPos = -1
                return
            }
            // 入れ替え完了後に最後に一度DBの更新をする
            val rearrangeList = adapter?.rearrangeGroupList(fromPos, toPos)
            rearrangeList?.let { groupListViewModel.update(rearrangeList) }
            // 移動位置情報を初期化
            fromPos = -1
            toPos = -1
        }
    }
}