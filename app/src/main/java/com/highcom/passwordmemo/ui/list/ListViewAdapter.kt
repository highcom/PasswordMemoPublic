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
import com.highcom.passwordmemo.util.TextSizeUtil
import com.highcom.passwordmemo.util.login.LoginDataManager
import java.util.Locale

class ListViewAdapter(
    context: Context,
    private var listData: List<Map<String?, *>?>?,
    private val loginDataManager: LoginDataManager?,
    private val adapterListener: AdapterListener
) : RecyclerView.Adapter<ListViewAdapter.ViewHolder?>(), Filterable {
    private val inflater: LayoutInflater
    private var orig: List<Map<String?, *>?>? = null
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

    private fun convertFromDpToPx(context: Context, dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

    override fun getItemCount(): Int {
        return if (listData != null) {
            listData!!.size + 1
        } else {
            0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position >= listData!!.size) {
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
        if (position >= listData!!.size) return
        val layoutHeight = layoutHeightMap[textSize.toInt()]
        if (layoutHeight != null) {
            val params = holder!!.rowLinearLayout?.layoutParams
            params?.height = layoutHeight.toInt()
            holder.rowLinearLayout?.layoutParams = params
        }
        val id = (listData!![position] as HashMap<*, *>?)!!["id"].toString()
        val title = (listData!![position] as HashMap<*, *>?)!!["title"].toString()
        val account = (listData!![position] as HashMap<*, *>?)!!["account"].toString()
        val password = (listData!![position] as HashMap<*, *>?)!!["password"].toString()
        val url = (listData!![position] as HashMap<*, *>?)!!["url"].toString()
        val groupId = (listData!![position] as HashMap<*, *>?)!!["group_id"].toString()
        val memo = (listData!![position] as HashMap<*, *>?)!!["memo"].toString()
        val date = (listData!![position] as HashMap<*, *>?)!!["inputdate"].toString()
        holder!!.id = java.lang.Long.valueOf(id)
        holder.title?.text = title
        holder.title?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
        holder.account = account
        holder.password = password
        holder.url = url
        holder.groupId = java.lang.Long.valueOf(groupId)
        holder.memo = memo
        holder.date?.text = date
        holder.date?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize - 3)
        // メモ表示が有効でメモが入力されている場合は表示する
        if (loginDataManager!!.memoVisibleSwitchEnable && memo != "") {
            holder.memoView?.visibility = View.VISIBLE
            holder.memoView?.text = memo
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
            override fun performFiltering(constraint: CharSequence): FilterResults {
                val oReturn = FilterResults()
                val results = ArrayList<Map<String?, *>?>()
                if (orig == null) orig = listData
                if (constraint != null) {
                    if (orig != null && orig!!.size > 0) {
                        for (g in orig!!) {
                            if (g!!["title"].toString().lowercase(Locale.getDefault())
                                    .contains(constraint.toString())
                            ) results.add(g)
                        }
                    }
                    oReturn.values = results
                } else {
                    oReturn.values = orig
                }
                return oReturn
            }

            override fun publishResults(
                constraint: CharSequence,
                results: FilterResults
            ) {
                listData = results.values as ArrayList<Map<String?, String?>?>
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        // private Context context;
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
        private const val ROW_LAYOUT_HEIGHT_SMALL = 40f
        private const val ROW_LAYOUT_HEIGHT_MEDIUM = 45f
        private const val ROW_LAYOUT_HEIGHT_LARGE = 50f
        private const val ROW_LAYOUT_HEIGHT_EXTRA_LARGE = 55f
    }
}