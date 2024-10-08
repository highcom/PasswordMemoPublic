package com.highcom.passwordmemo.util.login

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.ColorInt
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.util.BackgroundColorUtil
import com.highcom.passwordmemo.util.TextSizeUtil

/**
 * ログインデータ管理クラス
 *
 * @constructor
 * ログインデータ管理コンストラクタ
 *
 * @param application プリファレンス利用のためのアクティビティ
 */
class LoginDataManager private constructor(application: Application) {
    /** ログインデータ保存用のSharedPreferences */
    private val sharedPref: SharedPreferences
    /** ログイン用マスターパスワードユーティリティ */
    private val passUtil: MasterPasswordUtil
    /** マスターパスワード */
    var masterPassword: String? = null
        private set
    /** パスワード一覧のソートキー */
    var sortKey: String? = null
        private set
    /** 全データ削除スイッチの設定 */
    var deleteSwitchEnable = false
        private set
    /** 生体認証ログインスイッチの設定 */
    var biometricLoginSwitchEnable = false
        private set
    /** バックグラウンド時の非表示スイッチの設定 */
    var displayBackgroundSwitchEnable = false
        private set
    /** パスワード一覧にメモの表示設定 */
    var memoVisibleSwitchEnable = false
        private set
    /** 参照画面でのパスワード欄の初期表示設定 */
    var passwordVisibleSwitchEnable = false
        private set
    /** 背景色設定値 */
    var backgroundColor = 0
        private set
    /** テキストサイズ設定値 */
    var textSize = 0f
        private set
    /** クリップボードにコピーする方法の設定 */
    var copyClipboard = 0
        private set
    /** 選択グループID */
    var selectGroup: Long = 0
        private set

    init {
        sharedPref = application.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        passUtil = MasterPasswordUtil(sharedPref, application)
        checkBackgroundColor(application.applicationContext)
        updateSetting()
    }

    /**
     * 設定値の更新処理
     * * SharedPreferencesに保存されている値を変数に反映する
     *
     */
    fun updateSetting() {
        sortKey = sharedPref.getString("sortKey", "id")
        deleteSwitchEnable = sharedPref.getBoolean("deleteSwitchEnable", false)
        biometricLoginSwitchEnable = sharedPref.getBoolean("biometricLoginSwitchEnable", true)
        displayBackgroundSwitchEnable =
            sharedPref.getBoolean("displayBackgroundSwitchEnable", false)
        memoVisibleSwitchEnable = sharedPref.getBoolean("memoVisibleSwitchEnable", false)
        passwordVisibleSwitchEnable = sharedPref.getBoolean("passwordVisibleSwitchEnable", false)
        backgroundColor = sharedPref.getInt("backgroundColor", 0)
        textSize = sharedPref.getFloat("textSize", TextSizeUtil.TEXT_SIZE_MEDIUM.toFloat())
        copyClipboard = sharedPref.getInt("copyClipboard", 0)
        selectGroup = sharedPref.getLong("selectGroup", 1)
        try {
            masterPassword = passUtil.getMasterPasswordString("masterPassword")
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    /**
     * パスワード一覧のソートキー設定処理
     *
     * @param key 設定ソートキー値
     */
    fun setSortKey(key: String?) {
        sharedPref.edit().putString("sortKey", key).apply()
        sortKey = key
    }

    /**
     * 全データ削除スイッチの設定処理
     *
     * @param b 削除スイッチのON-OFF
     */
    fun setDeleteSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("deleteSwitchEnable", b).apply()
        updateSetting()
    }

    /**
     * 生体認証ログインスイッチの設定処理
     *
     * @param b 生体認証ログインスイッチのON-OFF
     */
    fun setBiometricLoginSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("biometricLoginSwitchEnable", b).apply()
        updateSetting()
    }

    /**
     * バックグラウンド時の非表示スイッチの設定処理
     *
     * @param b バックグラウンド時の非表示スイッチのON-OFF
     */
    fun setDisplayBackgroundSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("displayBackgroundSwitchEnable", b).apply()
        updateSetting()
    }

    /**
     * パスワード一覧にメモの表示設定処理
     *
     * @param b パスワード一覧にメモの表示設定ON-OFF
     */
    fun setMemoVisibleSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("memoVisibleSwitchEnable", b).apply()
        updateSetting()
    }

    /**
     * 参照画面でのパスワード欄の初期表示設定処理
     *
     * @param b 参照画面でのパスワード欄の初期表示設定ON-OFF
     */
    fun setPasswordVisibleSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("passwordVisibleSwitchEnable", b).apply()
        updateSetting()
    }

    /**
     * 背景色設定処理
     *
     * @param color 背景色
     */
    fun setBackgroundColor(@ColorInt color: Int) {
        sharedPref.edit().putInt("backgroundColor", color).apply()
        backgroundColor = color
    }

    /**
     * テキストサイズ設定処理
     *
     * @param size テキストサイズ
     */
    fun setTextSize(size: Float) {
        sharedPref.edit().putFloat("textSize", size).apply()
        textSize = size
    }

    /**
     * クリップボードにコピーする方法の設定処理
     *
     * @param operation クリップボードにコピーする方法の設定値(0:長押し 1:タップ)
     */
    fun setCopyClipboard(operation: Int) {
        sharedPref.edit().putInt("copyClipboard", operation).apply()
        copyClipboard = operation
    }

    /**
     * 選択グループ設定処理
     *
     * @param select 選択グループID
     */
    fun setSelectGroup(select: Long) {
        sharedPref.edit().putLong("selectGroup", select).apply()
        selectGroup = select
    }

    /** マスターパスワードが設定されているかどうか */
    val isMasterPasswordCreated: Boolean
        get() = masterPassword != null

    /**
     * マスターパスワード設定処理
     *
     * @param password マスターパスワード
     */
    fun setMasterPassword(password: String?) {
        try {
            passUtil.saveMasterPasswordString("masterPassword", password)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
        updateSetting()
    }

    /**
     * 全データ削除処理
     * * 全データを削除して、設定データを初期値で更新する
     *
     */
    fun clearAllData() {
        sharedPref.edit().clear().apply()
        updateSetting()
    }

    /**
     * 設計されて背景色の確認処理
     * * 設定されている背景色が無効値だった場合に初期値を設定する
     *
     * @param context コンテキスト
     */
    @Suppress("DEPRECATION")
    private fun checkBackgroundColor(context: Context) {
        val backgroundColorUtil = BackgroundColorUtil(context, null)
        if (!backgroundColorUtil.isColorExists(sharedPref.getInt("backgroundColor", 0))) {
            sharedPref.edit().putInt("backgroundColor", context.resources.getColor(R.color.white))
                .apply()
        }
    }

    companion object {
        /** ログインデータ管理インスタンス */
        private var manager: LoginDataManager? = null
        /** ログ出力用タグ */
        private const val TAG = "LoginDataManager"
        /** SharedPreferencesのファイル名 */
        private const val PREF_FILE_NAME = "com.highcom.LoginActivity.MasterPass"

        /**
         * ログイン管理インスタンス取得処理
         * * シングルトンのログイン管理インスタンスを取得する
         *
         * @param activity アクティビティ
         * @return ログイン管理インスタンス
         */
        fun getInstance(application: Application): LoginDataManager? {
            if (manager == null) {
                manager = LoginDataManager(application)
            }
            return manager
        }
    }
}