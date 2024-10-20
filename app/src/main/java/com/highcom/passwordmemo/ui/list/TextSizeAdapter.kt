package com.highcom.passwordmemo.ui.list

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.highcom.passwordmemo.databinding.RowTextSizeBinding

/**
 * テキストサイズ一覧表示用アダプタ
 *
 * @property layoutID テキストサイズ描画レイアウトID
 * @property names テキストサイズ名称一覧
 * @property sizes テキストサイズ
 * @constructor
 * テキストサイズ一覧表示用アダプタコンストラクタ
 *
 * @param context コンテキスト
 */
class TextSizeAdapter(
    context: Context,
    private val layoutID: Int,
    private val names: Array<String?>,
    private val sizes: IntArray
) : BaseAdapter() {
    private val inflater: LayoutInflater

    /**
     * テキストサイズ表示用ビューホルダークラス
     *
     */
    internal class TextSizeViewHolder {
        /** テキストサイズ表示ビュー */
        var textView: TextView? = null
    }

    init {
        inflater = LayoutInflater.from(context)
        context.resources
    }

    /**
     * テキストサイズ一覧数取得処理
     *
     * @return テキストサイズ一覧数
     */
    override fun getCount(): Int {
        return names.size
    }

    /**
     * 対象アイテム取得処理
     *
     * @param position 対象位置
     * @return 対象アイテム
     */
    override fun getItem(position: Int): Any {
        return position
    }

    /**
     * 対象アイテムID取得処理
     *
     * @param position 対象位置
     * @return 対象アイテムID
     */
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * 対象アイテムビュー取得処理
     * 対象のビューにテキストサイズ名称とテキストサイズを設定する
     *
     * @param position 対象位置
     * @param convertView 対象ビュー
     * @param parent 親のビューグループ
     * @return 生成したビュー
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var view = convertView
        val holder: TextSizeViewHolder
        if (view == null) {
            val binding = RowTextSizeBinding.inflate(inflater)
            view = binding.root
            holder = TextSizeViewHolder()
            holder.textView = binding.selectTextSizeView
            view.tag = holder
        } else {
            holder = view.tag as TextSizeViewHolder
        }
        holder.textView!!.text = names[position]
        holder.textView!!.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sizes[position].toFloat())
        return view
    }
}