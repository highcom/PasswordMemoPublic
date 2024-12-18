package com.highcom.passwordmemo.domain

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.AdapterView.OnItemClickListener
import androidx.annotation.ColorInt
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.databinding.AlertBackgroundColorBinding
import com.highcom.passwordmemo.ui.list.BackgroundColorAdapter
import com.highcom.passwordmemo.ui.list.BackgroundColorItem

/**
 * 背景色設定に関するユーティリティクラス
 *
 * @constructor
 * 背景色設定に関するユーティリティコンストラクタ
 *
 * @param context コンテキスト
 * @param listener 背景色設定通知用リスナー
 */
@Suppress("DEPRECATION")
class BackgroundColorUtil(context: Context, listener: BackgroundColorListener?) {
    /** 背景色一覧 */
    private val colors: IntArray = IntArray(8)
    /** 背景色設定通知用リスナー */
    private var backgroundColorListener: BackgroundColorListener?

    /**
     * 背景色設定通知用リスナークラス
     *
     */
    interface BackgroundColorListener {
        /**
         * 背景色選択処理
         *
         * @param color 選択背景色
         */
        fun onSelectColorClicked(color: Int)
    }

    init {
        colors[0] = context.resources.getColor(R.color.white)
        colors[1] = context.resources.getColor(R.color.lightgray)
        colors[2] = context.resources.getColor(R.color.lightcyan)
        colors[3] = context.resources.getColor(R.color.lavender)
        colors[4] = context.resources.getColor(R.color.bisque)
        colors[5] = context.resources.getColor(R.color.pink)
        colors[6] = context.resources.getColor(R.color.palegoldenrod)
        colors[7] = context.resources.getColor(R.color.palegreen)
        backgroundColorListener = listener
    }

    /**
     * 指定された背景色値があるかどうか
     *
     * @param color 指定した背景色
     * @return 指定された背景色の有無
     */
    fun isColorExists(@ColorInt color: Int): Boolean {
        for (i in 0..7) {
            if (colors[i] == color) return true
        }
        return false
    }

    /**
     * 背景色選択ダイアログ生成処理
     *
     * @param activity アクティビティ
     * @return 背景色選択ダイアログ
     */
    fun createBackgroundColorDialog(activity: Activity): AlertDialog {
        val binding = AlertBackgroundColorBinding.inflate(activity.layoutInflater)
        val alertDialog = AlertDialog.Builder(activity)
            .setView(binding.root)
            .create()
        alertDialog.show()
        val listView = binding.backgroundColorListView
        val colorItems: MutableList<BackgroundColorItem> = ArrayList()
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_white), colors[0]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lightgray), colors[1]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lightcyan), colors[2]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_lavender), colors[3]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_bisque), colors[4]))
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_pink), colors[5]))
        colorItems.add(
            BackgroundColorItem(
                activity.getString(R.string.color_palegoldenrod),
                colors[6]
            )
        )
        colorItems.add(BackgroundColorItem(activity.getString(R.string.color_palegreen), colors[7]))
        val adapter = BackgroundColorAdapter(activity, R.layout.row_background_color, colorItems)
        listView.adapter = adapter
        listView.onItemClickListener = OnItemClickListener { _, _, i, _ ->
            backgroundColorListener!!.onSelectColorClicked(colors[i])
            alertDialog.dismiss()
        }
        return alertDialog
    }
}