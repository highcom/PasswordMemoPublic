package com.highcom.passwordmemo.util

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.ui.list.TextSizeAdapter

/**
 * テキストサイズ設定に関するユーティリティクラス
 *
 * @constructor
 * テキストサイズ設定に関するユーティコンストラクタ
 *
 * @param context コンテキスト
 * @param listener テキストサイズ設定用リスナー
 */
class TextSizeUtil(context: Context, listener: TextSizeListener) {
    /** テキストサイズ名称一覧 */
    private val textNames: Array<String?> = arrayOfNulls(4)

    /**
     * テキストサイズ設定用リスナークラス
     *
     */
    interface TextSizeListener {
        /**
         * テキストサイズ選択処理
         *
         * @param size テキストサイズ
         */
        fun onTextSizeSelected(size: Float)
    }

    /** テキストサイズ設定用アダプタ */
    private val mTextSizeAdapter: TextSizeAdapter
    /** テキストサイズ設定用リスナー */
    private val mTextSizeListener: TextSizeListener

    init {
        textNames[0] = context.getString(R.string.size_small)
        textNames[1] = context.getString(R.string.size_medium)
        textNames[2] = context.getString(R.string.size_large)
        textNames[3] = context.getString(R.string.size_extra_large)
        mTextSizeAdapter = TextSizeAdapter(context, R.layout.row_text_size, textNames, textSizes)
        mTextSizeListener = listener
    }

    /**
     * テキストサイズ設定用スピナー生成処理
     *
     * @param spinner スピナー
     */
    fun createTextSizeSpinner(spinner: Spinner) {
        spinner.adapter = mTextSizeAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mTextSizeListener.onTextSizeSelected(textSizes[position].toFloat())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * スピナーの指定位置取得処理
     *
     * @param size テキストサイズ値
     * @return スピナー選択位置
     */
    fun getSpecifiedValuePosition(size: Float): Int {
        var position = 1 // デフォルト値である「中」の位置
        for (i in textSizes.indices) {
            if (textSizes[i].toFloat() == size) {
                position = i
                break
            }
        }
        return position
    }

    companion object {
        /** テキストサイズ小 */
        const val TEXT_SIZE_SMALL = 12
        /** テキストサイズ中 */
        const val TEXT_SIZE_MEDIUM = 15
        /** テキストサイズ大 */
        const val TEXT_SIZE_LARGE = 18
        /** テキストサイズ特大 */
        const val TEXT_SIZE_EXTRA_LARGE = 21
        /** テキストサイズ一覧 */
        private val textSizes =
            intArrayOf(TEXT_SIZE_SMALL, TEXT_SIZE_MEDIUM, TEXT_SIZE_LARGE, TEXT_SIZE_EXTRA_LARGE)
    }
}