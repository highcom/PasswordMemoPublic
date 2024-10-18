package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.highcom.passwordmemo.databinding.RowSpinnerSetTextsizeBinding

/**
 * テキストサイズ表示用アダプタ
 *
 * @property names テキストサイズ名称一覧
 * @property size テキストサイズ
 * @constructor
 * テキストサイズ表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class SetTextSizeAdapter(
    context: Context,
    private val names: List<String?>?,
    private val size: Int
) : BaseAdapter() {
    private val inflater: LayoutInflater

    /**
     * テキストサイズビューホルダー
     *
     */
    internal class TextSizeViewHolder {
        /** テキストサイズ名称ビュー */
        var textView: TextView? = null
    }

    init {
        inflater = LayoutInflater.from(context)
        context.resources
    }

    /**
     * テキストサイズ一覧数取得処理
     *
     * @return データ数
     */
    override fun getCount(): Int {
        return names!!.size
    }

    /**
     * テキストサイズアイテム取得処理
     *
     * @param position 取得位置
     * @return アイテムデータ
     */
    override fun getItem(position: Int): Any {
        return position
    }

    /**
     * テキストサイズID取得処理
     *
     * @param position 取得位置
     * @return アイテムID
     */
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * テキストサイズビュー取得処理
     *
     * @param position 取得位置
     * @param convertView 変換ビュー
     * @param parent 親のビューグループ
     * @return テキストサイズビュー
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val holder: TextSizeViewHolder
        if (view == null) {
            val binding = RowSpinnerSetTextsizeBinding.inflate(inflater)
            view = binding.root
            holder = TextSizeViewHolder()
            holder.textView = binding.selectSpinnerSetTextsizeView
            view.tag = holder
        } else {
            holder = view.tag as TextSizeViewHolder
        }
        holder.textView!!.text = names!![position]
        holder.textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size.toFloat())
        return view
    }
}