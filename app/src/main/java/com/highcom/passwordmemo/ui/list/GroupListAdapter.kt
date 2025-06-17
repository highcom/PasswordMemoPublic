package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.databinding.RowFooterBinding
import com.highcom.passwordmemo.databinding.RowGroupBinding
import com.highcom.passwordmemo.domain.SelectColorUtil
import com.highcom.passwordmemo.domain.TextSizeUtil

/**
 * グループ一覧表示用アダプタ
 *
 * @property lifecycleOwner ライフサイクル
 * @property adapterListener グループ一覧表示用アダプタリスナーインスタンス
 * @constructor
 * グループ一覧表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class GroupListAdapter(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val adapterListener: GroupAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    /** グループビューホルダーのbinding */
    private var groupBinding: RowGroupBinding? = null
    /** フッタービューホルダーのbinding */
    private var footerBinding: RowFooterBinding? = null
    /** レイアウト高さ設定マップデータ */
    private val layoutHeightMap: Map<Int, Float>
    /** 表示用グループ一覧データ */
    var groupList: List<GroupEntity>? = null
    /** 表示テキストサイズ */
    var textSize = 15f
    /** 編集モードかどうか */
    var editEnable = true

    /**
     * グループ一覧表示用アダプタのリスナー
     */
    interface GroupAdapterListener {
        /**
         * グループ名称選択イベント
         *
         * @param view 選択対象ビュー
         * @param groupId グループID
         */
        fun onGroupNameClicked(view: View, groupId: Long?)

        /**
         * グループ名称からフォーカスが外れたイベント
         *
         * @param view 選択対象ビュー
         * @param groupEntity グループデータ
         */
        fun onGroupNameOutOfFocused(view: View, groupEntity: GroupEntity)
    }

    /**
     * グループ一覧表示用ビューホルダー
     *
     * @constructor
     * コンストラクタ
     *
     * @param binding 表示アイテムバインド
     */
    inner class RowGroupViewHolder(val binding: RowGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        /**
         * グループデータバインド処理
         *
         * @param groupEntity グループデータエンティティ
         */
        fun bind(groupEntity: GroupEntity) {
            binding.groupEntity = groupEntity
            binding.executePendingBindings()
        }
        /** 編集前のグループ名称 */
        var orgGroupName = ""
    }

    /**
     * グループ一覧フッター用ビューホルダー
     *
     * @property binding
     */
    inner class RowFooterViewHolder(private val binding: RowFooterBinding) : RecyclerView.ViewHolder(binding.root)

    init {
        layoutHeightMap = object : HashMap<Int, Float>() {
            init {
                put(
                    TextSizeUtil.TEXT_SIZE_SMALL,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_SMALL)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_MEDIUM,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_MEDIUM)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_LARGE)
                )
                put(
                    TextSizeUtil.TEXT_SIZE_EXTRA_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_EXTRA_LARGE)
                )
            }
        }
    }

    /**
     * dpからpxへの変換処理
     *
     * @param context コンテキスト
     * @param dp 変換元dp値
     * @return 変換後px値
     */
    private fun convertFromDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    /**
     * グループ一覧データ数取得処理
     *
     * @return データ数+フッター行
     */
    override fun getItemCount(): Int {
        // フッターがあるので最低でも1を返す
        return if (groupList != null) {
            groupList!!.size + 1
        } else {
            0
        }
    }

    /**
     * ビュー種別取得処理
     *
     * @param position ビューの位置
     * @return ビュー種別
     */
    override fun getItemViewType(position: Int): Int {
        return if (position >= groupList!!.size) {
            TYPE_FOOTER
        } else TYPE_ITEM
    }

    /**
     * ビューホルダー生成処理
     *
     * @param parent 親のビューグループ
     * @param viewType ビュー種別
     * @return 生成したビューホルダー
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                groupBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.row_group, parent, false)
                groupBinding?.lifecycleOwner = lifecycleOwner
                RowGroupViewHolder(groupBinding!!)
            }
            TYPE_FOOTER -> {
                footerBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.row_footer, parent, false)
                footerBinding?.lifecycleOwner = lifecycleOwner
                RowFooterViewHolder(footerBinding!!)
            }
            else -> {
                groupBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.row_group, parent, false)
                groupBinding?.lifecycleOwner = lifecycleOwner
                RowGroupViewHolder(groupBinding!!)
            }
        }
    }

    /**
     * ビューホルダーのバインド処理
     *
     * @param holder ビューホルダー
     * @param position 表示位置
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is RowGroupViewHolder -> {
                groupList?.let { holder.bind(it[position]) }
                // レイアウト高さの設定
                val layoutHeight = layoutHeightMap[textSize.toInt()]
                if (layoutHeight != null) {
                    val params = holder.binding.rowGroupLayout.layoutParams
                    params?.height = layoutHeight.toInt()
                    holder.binding.rowGroupLayout.layoutParams = params
                }
                holder.binding.groupName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                // グループ名が空欄があった場合は新規追加時なのでフォーカスする
                if (editEnable && holder.binding.groupEntity?.name == "") {
                    adapterListener.onGroupNameClicked(
                        holder.binding.groupName,
                        holder.binding.groupEntity?.groupId
                    )
                }
                // 編集モードではない状態でタップされた場合は選択されたとみなす
                holder.binding.folderIcon.setOnClickListener { view: View ->
                    // 安全色を設定
                    val colors = arrayListOf(
                        ColorItem(view.context.getString(R.string.safe_color_none), ContextCompat.getColor(view.context, R.color.clear)),
                        ColorItem(view.context.getString(R.string.safe_color_red), ContextCompat.getColor(view.context, R.color.safe_red)),
                        ColorItem(view.context.getString(R.string.safe_color_yellow_red), ContextCompat.getColor(view.context, R.color.safe_yellow_red)),
                        ColorItem(view.context.getString(R.string.safe_color_yellow), ContextCompat.getColor(view.context, R.color.safe_yellow)),
                        ColorItem(view.context.getString(R.string.safe_color_green), ContextCompat.getColor(view.context, R.color.safe_green)),
                        ColorItem(view.context.getString(R.string.safe_color_blue), ContextCompat.getColor(view.context, R.color.safe_blue)),
                        ColorItem(view.context.getString(R.string.safe_color_purple), ContextCompat.getColor(view.context, R.color.safe_purple))
                    )
                    val selectColorUtil = SelectColorUtil(colors, object : SelectColorUtil.SelectColorListener {
                        /**
                         * 色選択処理
                         *
                         * @param color 選択色
                         */
                        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                        override fun onSelectColorClicked(color: Int) {
                            val drawable = ContextCompat.getDrawable(view.context, R.drawable.ic_folder)?.mutate()
                            if (color == ContextCompat.getColor(view.context, R.color.clear)) {
                                drawable?.setTintList(null)
                            } else {
                                drawable?.setTint(color)
                            }
                            holder.binding.folderIcon.setImageDrawable(drawable)
                        }
                    })
                    selectColorUtil.createSelectColorDialog(view.context)
                }
                // グループ名称を選択された場合はイベントを発生
                holder.binding.groupName.setOnClickListener { view: View ->
                    holder.orgGroupName = holder.binding.groupName.text.toString()
                    adapterListener.onGroupNameClicked(view, holder.binding.groupEntity?.groupId)
                }
                // グループ名称のフォーカス変更についての処理
                holder.binding.groupName.onFocusChangeListener = OnFocusChangeListener { view: View, b: Boolean ->
                    if (b) {
                        holder.orgGroupName = holder.binding.groupName.text.toString()
                        adapterListener.onGroupNameClicked(view, holder.binding.groupEntity?.groupId)
                    } else {
                        // グループ名を空欄で登録することは出来ないので元の名前にする
                        if (holder.binding.groupName.text.toString() == "") {
                            holder.binding.groupName.setText(holder.orgGroupName)
                        }
                        // フォーカスが外れた時に内容が変更されていたら更新する
                        adapterListener.onGroupNameOutOfFocused(view, holder.binding.groupEntity!!)
                    }
                }
                // キーボードのエンターが押下された場合
                holder.binding.groupName.setOnEditorActionListener { textView, i, _ ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        textView.isFocusable = false
                        textView.isFocusableInTouchMode = false
                        textView.requestFocus()
                    }
                    false
                }
                // 編集モードでも1番目の「すべて」は並べ替えさせない
                if (editEnable && holder.binding.groupEntity?.groupId != 1L) {
                    holder.binding.groupRearrangeButton.visibility = View.VISIBLE
                } else {
                    holder.binding.groupRearrangeButton.visibility = View.GONE
                }
            }
            is RowFooterViewHolder -> {}
        }
    }

    /**
     * グループデータ一覧の並べ替え処理
     *
     * @param fromPos 移動元の位置
     * @param toPos 移動先の位置
     * @return 並べ替え後のグループ一覧データ
     */
    fun rearrangeGroupList(fromPos: Int, toPos: Int): List<GroupEntity> {
        val rearrangeList: ArrayList<GroupEntity> = ArrayList()
        groupList?.let {
            for (entity in it) {
                rearrangeList.add(entity)
            }
        }
        // 引数で渡された位置で並べ替え
        val fromEntity = rearrangeList[fromPos]
        rearrangeList.removeAt(fromPos)
        rearrangeList.add(toPos, fromEntity)
        // 再度順番を振り直す
        var order = 1
        for (entity in rearrangeList) {
            entity.groupOrder = order
            order++
        }
        groupList = rearrangeList
        return rearrangeList
    }

    companion object {
        /** アダプタの種別が通常のアイテム */
        private const val TYPE_ITEM = 1
        /** アダプタの種別がフッター */
        private const val TYPE_FOOTER = 2
        /** テキストサイズが小の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_SMALL = 40f
        /** テキストサイズが中の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_MEDIUM = 45f
        /** テキストサイズが大の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_LARGE = 53f
        /** テキストサイズが特大の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 60f
    }
}