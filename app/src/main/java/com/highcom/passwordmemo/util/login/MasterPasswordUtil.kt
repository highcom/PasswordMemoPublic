package com.highcom.passwordmemo.util.login

import android.content.Context
import android.content.SharedPreferences
import com.highcom.passwordmemo.R

class MasterPasswordUtil internal constructor(private var sharedPref: SharedPreferences, context: Context) {
    // 暗号化/復号キー
    private val secretKey: String

    init {
        secretKey = context.getString(R.string.master_secret_key)
    }

    // 暗号化してパスワードを保存
    @Throws(Exception::class)
    fun saveMasterPasswordString(key: String?, value: String?) {
        if (key.isNullOrEmpty()) {
            throw Exception("キーが空です。")
        }
        if (value == null) {
            throw Exception("値が空です。")
        }
        // 暗号化
        val cryptUtil = CryptUtil()
        val encValue = cryptUtil.encrypt(value, secretKey)

        // 保存
        val editor = sharedPref.edit()
        editor.putString(key, encValue).apply()
    }

    // 複合化してパスワードを読み込み
    @Throws(Exception::class)
    fun getMasterPasswordString(key: String?): String? {
        // 値取得
        if (key.isNullOrEmpty()) {
            throw Exception("キーが空です。")
        }
        val value = sharedPref.getString(key, null) ?: return null

        // 復号
        val cryptUtil = CryptUtil()
        return cryptUtil.decrypt(value, secretKey)
    }
}