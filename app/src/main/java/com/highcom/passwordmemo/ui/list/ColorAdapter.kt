package com.highcom.passwordmemo.ui.list

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.highcom.passwordmemo.databinding.RowColorBinding

/**
 * 色一覧表示用アダプタ
 *
 * @property mResource リソースID
 * @property mItems 色一覧データ
 * @constructor
 * 色一覧表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class ColorAdapter(
    context: Context,
    private val mResource: Int,
    private val mItems: List<ColorItem>
) : ArrayAdapter<ColorItem?>(context, mResource, mItems) {
    private val mInflater: LayoutInflater

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = RowColorBinding.inflate(mInflater)
        val view: View = convertView ?: binding.root

        // リストビューに表示する要素を取得
        val item = mItems[position]

        // 色と名前を設定
        val title = binding.colorName
        title.text = item.colorName
        val linearLayout = binding.colorName
        item.colorCode?.let { linearLayout.setBackgroundColor(it) }
        return view
    }
}