package com.highcom.passwordmemo.domain

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * ダークモード管理ユーティリティ
 */
object DarkModeUtil {

    /** ダークモード設定: OFF */
    const val DARK_MODE_OFF = 0
    /** ダークモード設定: ON */
    const val DARK_MODE_ON = 1
    /** ダークモード設定: AUTO */
    const val DARK_MODE_AUTO = 2

    /**
     * ダークモード設定を適用する
     *
     * @param context コンテキスト
     * @param darkModeSetting ダークモード設定値
     */
    fun applyDarkMode(context: Context, darkModeSetting: Int) {
        when (darkModeSetting) {
            DARK_MODE_OFF -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            DARK_MODE_ON -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            DARK_MODE_AUTO -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    /**
     * 現在のダークモード設定が有効かどうかを判定する
     *
     * @param context コンテキスト
     * @param darkModeSetting ダークモード設定値
     * @return ダークモードが有効な場合はtrue
     */
    fun isDarkModeEnabled(context: Context, darkModeSetting: Int): Boolean {
        return when (darkModeSetting) {
            DARK_MODE_OFF -> false
            DARK_MODE_ON -> true
            DARK_MODE_AUTO -> isSystemDarkModeEnabled(context)
            else -> false
        }
    }

    /**
     * システムのダークモード設定が有効かどうかを判定する
     *
     * @param context コンテキスト
     * @return システムがダークモードの場合はtrue
     */
    private fun isSystemDarkModeEnabled(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * アクティビティのテーマを再適用する
     *
     * @param activity 再適用するアクティビティ
     */
    fun recreateActivity(activity: Activity) {
        activity.recreate()
    }
}