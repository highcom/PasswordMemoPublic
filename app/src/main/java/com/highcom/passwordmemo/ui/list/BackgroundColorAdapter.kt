package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.highcom.passwordmemo.R

/**
 * 背景色一覧表示用アダプタ
 *
 * @property mResource リソースID
 * @property mItems 背景色一覧データ
 * @constructor
 * 背景色一覧表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class BackgroundColorAdapter(
    context: Context,
    private val mResource: Int,
    private val mItems: List<BackgroundColorItem>
) : ArrayAdapter<BackgroundColorItem?>(context, mResource, mItems) {
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = convertView ?: mInflater.inflate(mResource, null)

        // リストビューに表示する要素を取得
        val item = mItems[position]

        // 背景色と名前を設定
        val title = view.findViewById<TextView>(R.id.backgroundColorName)
        title.text = item.colorName
        val linearLayout = view.findViewById<LinearLayout>(R.id.backgroundColorRow)
        item.colorCode?.let { linearLayout.setBackgroundColor(it) }
        return view
    }
}