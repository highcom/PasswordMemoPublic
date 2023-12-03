package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.highcom.passwordmemo.R

class SetTextSizeAdapter(
    context: Context,
    private val names: List<String?>?,
    private val size: Int
) : BaseAdapter() {
    private val inflater: LayoutInflater

    internal class TextSizeViewHolder {
        var textView: TextView? = null
    }

    init {
        inflater = LayoutInflater.from(context)
        val res = context.resources
    }

    override fun getCount(): Int {
        return names!!.size
    }

    override fun getItem(position: Int): Any {
        return position
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var convertView = convertView
        val holder: TextSizeViewHolder
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_spinner_set_textsize, null)
            holder = TextSizeViewHolder()
            holder.textView = convertView.findViewById(R.id.select_spinner_set_textsize_view)
            convertView.tag = holder
        } else {
            holder = convertView.tag as TextSizeViewHolder
        }
        holder.textView!!.text = names!![position]
        holder.textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
        return convertView
    }
}