package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.util.TextSizeUtil
import com.highcom.passwordmemo.util.login.LoginDataManager
import java.util.Locale

class PasswordListAdapter(
    context: Context,
    private val loginDataManager: LoginDataManager?,
    private val adapterListener: AdapterListener
) : RecyclerView.Adapter<PasswordListAdapter.ViewHolder?>(), Filterable {
    private val inflater: LayoutInflater
    /** DBに登録されているパスワード一覧データ */
    private var origPasswordList: List<PasswordEntity>? = null
    /** ソートやフィルタされた表示用のパスワード一覧データ */
    private var passwordList: List<PasswordEntity>? = null
    /** ソート種別 */
    private var sortType: String? = null
    private val layoutHeightMap: Map<Int, Float>
    var textSize = 15f
    var editEnable = false

    interface AdapterListener {
        fun onAdapterClicked(view: View)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var rowLinearLayout: LinearLayout? = null
        var id: Long? = null
        var imageButton: ImageButton? = null
        var title: TextView? = null
        var account: String? = null
        var password: String? = null
        var url: String? = null
        var groupId: Long? = null
        var memo: String? = null
        var date: TextView? = null
        var rearrangebtn: ImageButton? = null
        var memoView: TextView? = null

        init {
            // フッターの場合には何も設定しない
            if (itemView.id != R.id.row_footer) {
                rowLinearLayout = itemView.findViewById<View>(R.id.rowLinearLayout) as LinearLayout
                imageButton = itemView.findViewById<View>(R.id.round_key_icon) as ImageButton
                title = itemView.findViewById<View>(R.id.title) as TextView
                date = itemView.findViewById<View>(R.id.date) as TextView
                rearrangebtn = itemView.findViewById<View>(R.id.rearrangebutton) as ImageButton
                memoView = itemView.findViewById<View>(R.id.memoView) as TextView
                if (editEnable) {
                    rearrangebtn?.visibility = View.VISIBLE
                } else {
                    rearrangebtn?.visibility = View.GONE
                }
            }
        }
    }

    init {
        inflater = LayoutInflater.from(context)
        layoutHeightMap = object : HashMap<Int, Float>() {
            init {
                put(
                    TextSizeUtil.Companion.TEXT_SIZE_SMALL,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_SMALL)
                )
                put(
                    TextSizeUtil.Companion.TEXT_SIZE_MEDIUM,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_MEDIUM)
                )
                put(
                    TextSizeUtil.Companion.TEXT_SIZE_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_LARGE)
                )
                put(
                    TextSizeUtil.Companion.TEXT_SIZE_EXTRA_LARGE,
                    convertFromDpToPx(context, ROW_LAYOUT_HEIGHT_EXTRA_LARGE)
                )
            }
        }
    }

    fun setList(list: List<PasswordEntity>) {
        origPasswordList = list
        passwordList = origPasswordList
    }

    private fun convertFromDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun getItemCount(): Int {
        return if (passwordList != null) {
            passwordList!!.size + 1
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position >= passwordList!!.size) {
            TYPE_FOOTER
        } else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == TYPE_ITEM) {
            ViewHolder(
                inflater.inflate(
                    R.layout.row,
                    parent,
                    false
                )
            )
        } else if (viewType == TYPE_FOOTER) {
            ViewHolder(
                inflater.inflate(
                    R.layout.row_footer,
                    parent,
                    false
                )
            )
        } else {
            ViewHolder(
                inflater.inflate(
                    R.layout.row,
                    parent,
                    false
                )
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // フッターの場合にはデータをバインドしない
        if (position >= passwordList!!.size) return
        val layoutHeight = layoutHeightMap[textSize.toInt()]
        if (layoutHeight != null) {
            val params = holder!!.rowLinearLayout?.layoutParams
            params?.height = layoutHeight.toInt()
            holder.rowLinearLayout?.layoutParams = params
        }
        passwordList?.let {
            holder.id = it[position].id
            holder.title?.text = it[position].title
            holder.title?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            holder.account = it[position].account
            holder.password = it[position].password
            holder.url = it[position].url
            holder.groupId = it[position].groupId
            holder.memo = it[position].memo
            holder.date?.text = it[position].inputDate
            holder.date?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize - 3)
        }
        // メモ表示が有効でメモが入力されている場合は表示する
        if (loginDataManager!!.memoVisibleSwitchEnable && holder.memo != "") {
            holder.memoView?.visibility = View.VISIBLE
            holder.memoView?.text = holder.memo
        } else {
            holder.memoView?.visibility = View.GONE
            holder.memoView?.text = ""
        }
        holder.memoView?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize - 3)
        holder.itemView.tag = holder
        // アイテムクリック時ののイベントを追加
        holder.imageButton?.setOnClickListener { view ->
            val parentView = view.parent as View
            parentView.callOnClick()
        }
        holder.itemView.setOnClickListener { view -> adapterListener.onAdapterClicked(view) }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val oReturn = FilterResults()
                val results = ArrayList<PasswordEntity>()
                if (origPasswordList == null) origPasswordList = passwordList
                if (constraint != null) {
                    if (origPasswordList != null && origPasswordList!!.size > 0) {
                        for (entity in origPasswordList!!) {
                            if (entity.title.lowercase(Locale.getDefault())
                                    .contains(constraint.toString())
                            ) results.add(entity)
                        }
                    }
                    oReturn.values = results
                } else {
                    oReturn.values = origPasswordList
                }
                return oReturn
            }

            override fun publishResults(
                constraint: CharSequence?,
                results: FilterResults?
            ) {
                passwordList = results?.values as List<PasswordEntity>
                notifyDataSetChanged()
            }
        }
    }

    /**
     * パスワードデータ一覧の並べ替え処理
     *
     * @param fromPos 移動元の位置
     * @param toPos 移動先の位置
     * @return 並べ替え後のパスワード一覧データ
     */
    fun rearrangePasswordList(fromPos: Int, toPos: Int): List<PasswordEntity> {
        val origPasswordIds = ArrayList<Long>()
        val rearrangePasswordList = ArrayList<PasswordEntity>()
        // 元のIDの並びを保持と並べ替えができるリストに入れ替える
        origPasswordList?.let {
            for (entity in it) {
                origPasswordIds.add(entity.id)
                rearrangePasswordList.add(entity)
            }
        }
        // 引数で渡された位置で並べ替え
        val fromPassword = rearrangePasswordList[fromPos]
        rearrangePasswordList.removeAt(fromPos)
        rearrangePasswordList.add(toPos, fromPassword)
        // 再度IDを振り直す
        val itr = origPasswordIds.listIterator()
        for (comic in rearrangePasswordList) {
            comic.id = itr.next()
        }

        return rearrangePasswordList
    }

    /**
     * パスワードデータ一覧のソート処理
     *
     * @param key ソート種別
     */
    fun sortPasswordList(key: String?) {
        sortType = key
        // 比較処理の実装
        val comparator = Comparator<PasswordEntity> { t1, t2 ->
            var result = when(sortType) {
                SORT_ID -> t1.id.compareTo(t2.id)
                SORT_TITLE -> t1.title.compareTo(t2.title)
                SORT_INPUTDATE -> t1.inputDate.compareTo(t2.inputDate)
                else -> t1.id.compareTo(t2.id)
            }

            // ソート順が決まらない場合には、idで比較する
            if (result == 0) {
                result = t1.id.compareTo(t2.id)
            }
            return@Comparator result
        }
        passwordList = passwordList?.sortedWith(comparator)
    }

    companion object {
        const val SORT_ID = "id"
        const val SORT_TITLE = "title"
        const val SORT_INPUTDATE = "inputdate"
        // private Context context;
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
        private const val ROW_LAYOUT_HEIGHT_SMALL = 40f
        private const val ROW_LAYOUT_HEIGHT_MEDIUM = 45f
        private const val ROW_LAYOUT_HEIGHT_LARGE = 50f
        private const val ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 55f
    }
}