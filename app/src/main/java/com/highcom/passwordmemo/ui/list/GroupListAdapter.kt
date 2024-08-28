package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.ui.list.GroupListAdapter.GroupViewHolder
import com.highcom.passwordmemo.util.TextSizeUtil

/**
 * グループ一覧表示用アダプタ
 *
 * @property adapterListener グループ一覧表示用アダプタリスナーインスタンス
 * @constructor
 * グループ一覧表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class GroupListAdapter(
    context: Context,
    private val adapterListener: GroupAdapterListener
) : RecyclerView.Adapter<GroupViewHolder?>() {
    private val inflater: LayoutInflater
    /** レイアウト高さ設定マップデータ */
    private val layoutHeightMap: Map<Int, Float>
    /** 表示用グループ一覧データ */
    var groupList: List<GroupEntity>? = null
    /** 表示テキストサイズ */
    var textSize = 15f
    /** 編集モードかどうか */
    var editEnable = false

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
     * @param itemView 表示アイテムビュー
     */
    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** グループデータ表示用レイアウト */
        var rowLinearLayout: LinearLayout? = null
        /** グループID */
        var groupId: Long? = null
        /** グループデータの並び順 */
        var groupOrder = 0
        /** グループアイコン画像 */
        private var groupImage: ImageButton? = null
        /** グループ名称 */
        var groupName: EditText? = null
        /** 編集前のグループ名称 */
        private var orgGroupName = ""
        /** 並べ替えアイコン画像 */
        var groupRearrangeButton: ImageButton? = null

        init {
            // フッターの場合には何も設定しない
            if (itemView.id != R.id.row_footer) {
                rowLinearLayout = itemView.findViewById<View>(R.id.rowGroupLayout) as LinearLayout
                groupImage = itemView.findViewById<View>(R.id.folder_icon) as ImageButton
                groupName = itemView.findViewById<View>(R.id.groupName) as EditText
                groupImage?.setOnClickListener { view: View ->
                    if (!editEnable) {
                        orgGroupName = groupName?.text.toString()
                        adapterListener.onGroupNameClicked(view, groupId)
                    }
                }
                groupName?.setOnClickListener { view: View ->
                    orgGroupName = groupName?.text.toString()
                    adapterListener.onGroupNameClicked(view, groupId)
                }
                groupName?.onFocusChangeListener = OnFocusChangeListener { view: View, b: Boolean ->
                    if (b) {
                        orgGroupName = groupName?.text.toString()
                        adapterListener.onGroupNameClicked(view, groupId)
                    } else {
                        // グループ名を空欄で登録することは出来ないので元の名前にする
                        if (groupName?.text.toString() == "") {
                            groupName?.setText(orgGroupName)
                        }
                        val groupEntity = GroupEntity(
                            groupId = groupId!!,
                            groupOrder = groupOrder,
                            name = groupName?.text.toString()
                        )
                        // フォーカスが外れた時に内容が変更されていたら更新する
                        adapterListener.onGroupNameOutOfFocused(view, groupEntity)
                    }
                }
                // キーボードのエンターが押下された場合
                groupName?.setOnEditorActionListener { textView, i, _ ->
                    if (i == EditorInfo.IME_ACTION_DONE) {
                        textView.isFocusable = false
                        textView.isFocusableInTouchMode = false
                        textView.requestFocus()
                    }
                    false
                }
                groupRearrangeButton = itemView.findViewById(R.id.groupRearrangeButton)
            }
        }
    }

    init {
        inflater = LayoutInflater.from(context)
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return when (viewType) {
            TYPE_ITEM -> {
                GroupViewHolder(inflater.inflate(R.layout.row_group, parent, false))
            }
            TYPE_FOOTER -> {
                GroupViewHolder(inflater.inflate(R.layout.row_footer, parent, false))
            }
            else -> {
                GroupViewHolder(inflater.inflate(R.layout.row_group, parent, false))
            }
        }
    }

    /**
     * ビューホルダーのバインド処理
     *
     * @param holder ビューホルダー
     * @param position 表示位置
     */
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        // フッターの場合にはデータをバインドしない
        if (position >= groupList!!.size) return
        val layoutHeight = layoutHeightMap[textSize.toInt()]
        if (layoutHeight != null) {
            val params = holder.rowLinearLayout?.layoutParams
            params?.height = layoutHeight.toInt()
            holder.rowLinearLayout?.layoutParams = params
        }
        groupList?.let {
            holder.groupId = it[position].groupId
            holder.groupOrder = it[position].groupOrder
            holder.groupName?.setText(it[position].name)
            holder.groupName?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            holder.itemView.tag = holder
            // グループ名が空欄があった場合は新規追加時なのでフォーカスする
            if (editEnable && it[position].name == "") {
                adapterListener.onGroupNameClicked(
                    holder.itemView.findViewById(R.id.groupName),
                    holder.groupId
                )
            }
        }
        // 編集モードでも1番目の「すべて」は並べ替えさせない
        if (editEnable && holder.groupId != null && holder.groupId != 1L) {
            holder.groupRearrangeButton?.visibility = View.VISIBLE
        } else {
            holder.groupRearrangeButton?.visibility = View.GONE
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
        val fromentity = rearrangeList[fromPos]
        rearrangeList.removeAt(fromPos)
        rearrangeList.add(toPos, fromentity)
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
        private const val ROW_LAYOUT_HEIGHT_LARGE = 50f
        /** テキストサイズが特大の場合の高さ */
        private const val ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 55f
    }
}