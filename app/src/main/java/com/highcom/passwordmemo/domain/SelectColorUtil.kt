package com.highcom.passwordmemo.domain

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.ColorInt
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.AlertSelectColorBinding
import com.highcom.passwordmemo.ui.list.ColorAdapter
import com.highcom.passwordmemo.ui.list.ColorItem

/**
 * 色設定に関するユーティリティクラス
 *
 * @constructor
 * 色設定に関するユーティリティコンストラクタ
 *
 * @param colors 色一覧
 * @param listener 色設定通知用リスナー
 */
@Suppress("DEPRECATION")
class SelectColorUtil(val colors: List<ColorItem>, listener: SelectColorListener?) {
    /** 色設定通知用リスナー */
    private var selectColorListener: SelectColorListener?

    /**
     * 色設定通知用リスナークラス
     *
     */
    interface SelectColorListener {
        /**
         * 色選択処理
         *
         * @param color 選択色
         */
        fun onSelectColorClicked(color: Int)
    }

    init {
        selectColorListener = listener
    }

    /**
     * 指定された色値があるかどうか
     *
     * @param select 指定した色
     * @return 指定された色の有無
     */
    fun isColorExists(@ColorInt select: Int): Boolean {
        for (color in colors) {
            if (color.colorCode == select) return true
        }
        return false
    }

    /**
     * 色選択ダイアログ生成処理
     *
     * @param context コンテキスト
     * @return 色選択ダイアログ
     */
    fun createSelectColorDialog(context: Context): AlertDialog {
        val binding = AlertSelectColorBinding.inflate(LayoutInflater.from(context))
        val alertDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()
        alertDialog.show()
        val listView = binding.colorListView
        val adapter = ColorAdapter(context, R.layout.row_color, colors)
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            colors[i].colorCode?.let { selectColorListener?.onSelectColorClicked(it) }
            alertDialog.dismiss()
        }
        return alertDialog
    }
}