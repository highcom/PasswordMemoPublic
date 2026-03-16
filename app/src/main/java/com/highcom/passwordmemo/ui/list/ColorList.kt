package com.highcom.passwordmemo.ui.list

import androidx.core.content.ContextCompat
import com.highcom.passwordmemo.PasswordMemoApplication
import com.highcom.passwordmemo.R

/**
 * 色設定用アイテムリスト
 */
object ColorList {
    /** 背景色 */
    val backgroundColors: List<ColorItem> by lazy {
        listOf(
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_white), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.white)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_lightgray), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.lightgray)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_lightcyan), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.lightcyan)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_lavender), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.lavender)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_bisque), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.bisque)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_pink), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pink)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_palegoldenrod), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.palegoldenrod)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.color_palegreen), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.palegreen))
        )
    }
    /** アイコン色 */
    val iconColors: List<ColorItem> by lazy {
        listOf(
            // 1列目：安全色等 / 2列目：パステルカラー
            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_none), 0),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_creme), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_creme)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_red), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_red)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_pink), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_pink)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_yellow_red), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_yellow_red)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_yellow_red), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_yellow_red)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_yellow), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_yellow)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_yellow), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_yellow)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_green), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_green)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_green), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_green)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_blue), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_blue)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_blue), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_blue)),

            ColorItem(PasswordMemoApplication.instance.getString(R.string.safe_color_purple), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.safe_purple)),
            ColorItem(PasswordMemoApplication.instance.getString(R.string.pastel_color_purple), ContextCompat.getColor(PasswordMemoApplication.instance, R.color.pastel_purple))
        )
    }
}