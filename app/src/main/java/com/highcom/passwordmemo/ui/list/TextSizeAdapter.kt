package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.highcom.passwordmemo.R

class TextSizeAdapter(
    context: Context,
    private val layoutID: Int,
    private val names: Array<String?>,
    private val sizes: IntArray
) : BaseAdapter() {
    private val inflater: LayoutInflater
    private val mColor = 0

    internal class TextSizeViewHolder {
        var textView: TextView? = null
    }

    init {
        inflater = LayoutInflater.from(context)
        val res = context.resources
    }

    override fun getCount(): Int {
        return names.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView
        val holder: TextSizeViewHolder
        if (convertView == null) {
            convertView = inflater.inflate(layoutID, null)
            holder = TextSizeViewHolder()
            holder.textView = convertView.findViewById(R.id.select_text_size_view)
            convertView.tag = holder
        } else {
            holder = convertView.tag as TextSizeViewHolder
        }
        holder.textView!!.text = names[position]
        holder.textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizes[position].toFloat())
        return convertView!!
    }
}