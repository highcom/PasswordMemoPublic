package com.highcom.passwordmemo.util.login

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.ColorInt
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.util.BackgroundColorUtil
import com.highcom.passwordmemo.util.TextSizeUtil

class LoginDataManager private constructor(activity: Activity) {
    private val sharedPref: SharedPreferences
    private val passUtil: MasterPasswordUtil
    private var masterPassword: String? = null
    private var sortKey: String? = null
    private var deleteSwitchEnable = false
    private var biometricLoginSwitchEnable = false
    private var displayBackgroundSwitchEnable = false
    private var memoVisibleSwitchEnable = false
    private var passwordVisibleSwitchEnable = false
    private var backgroundColor = 0
    private var textSize = 0f
    private var copyClipboard = 0
    private var selectGroup: Long = 0

    init {
        sharedPref = activity.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)
        passUtil = MasterPasswordUtil(sharedPref, activity)
        checkBackgroundColor(activity.applicationContext)
        updateSetting()
    }

    fun updateSetting() {
        sortKey = sharedPref.getString("sortKey", "id")
        deleteSwitchEnable = sharedPref.getBoolean("deleteSwitchEnable", false)
        biometricLoginSwitchEnable = sharedPref.getBoolean("biometricLoginSwitchEnable", true)
        displayBackgroundSwitchEnable =
            sharedPref.getBoolean("displayBackgroundSwitchEnable", false)
        memoVisibleSwitchEnable = sharedPref.getBoolean("memoVisibleSwitchEnable", false)
        passwordVisibleSwitchEnable = sharedPref.getBoolean("passwordVisibleSwitchEnable", false)
        backgroundColor = sharedPref.getInt("backgroundColor", 0)
        textSize =
            sharedPref.getFloat("textSize", TextSizeUtil.Companion.TEXT_SIZE_MEDIUM.toFloat())
        copyClipboard = sharedPref.getInt("copyClipboard", 0)
        selectGroup = sharedPref.getLong("selectGroup", 1)
        try {
            masterPassword = passUtil.getMasterPasswordString("masterPassword")
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
    }

    fun getSortKey(): String? {
        return sortKey
    }

    fun setSortKey(key: String?) {
        sharedPref.edit().putString("sortKey", key).apply()
        sortKey = key
    }

    fun isDeleteSwitchEnable(): Boolean {
        return deleteSwitchEnable
    }

    fun setDeleteSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("deleteSwitchEnable", b).apply()
        updateSetting()
    }

    fun isBiometricLoginSwitchEnable(): Boolean {
        return biometricLoginSwitchEnable
    }

    fun setBiometricLoginSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("biometricLoginSwitchEnable", b).apply()
        updateSetting()
    }

    fun isDisplayBackgroundSwitchEnable(): Boolean {
        return displayBackgroundSwitchEnable
    }

    fun setDisplayBackgroundSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("displayBackgroundSwitchEnable", b).apply()
        updateSetting()
    }

    fun isMemoVisibleSwitchEnable(): Boolean {
        return memoVisibleSwitchEnable
    }

    fun setMemoVisibleSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("memoVisibleSwitchEnable", b).apply()
        updateSetting()
    }

    fun isPasswordVisibleSwitchEnable(): Boolean {
        return passwordVisibleSwitchEnable
    }

    fun setPasswordVisibleSwitchEnable(b: Boolean) {
        sharedPref.edit().putBoolean("passwordVisibleSwitchEnable", b).apply()
        updateSetting()
    }

    @ColorInt
    fun getBackgroundColor(): Int {
        return backgroundColor
    }

    fun setBackgroundColor(@ColorInt color: Int) {
        sharedPref.edit().putInt("backgroundColor", color).apply()
        backgroundColor = color
    }

    fun getTextSize(): Float {
        return textSize
    }

    fun setTextSize(size: Float) {
        sharedPref.edit().putFloat("textSize", size).apply()
        textSize = size
    }

    fun getCopyClipboard(): Int {
        return copyClipboard
    }

    fun setCopyClipboard(operation: Int) {
        sharedPref.edit().putInt("copyClipboard", operation).apply()
        copyClipboard = operation
    }

    fun getSelectGroup(): Long {
        return selectGroup
    }

    fun setSelectGroup(select: Long) {
        sharedPref.edit().putLong("selectGroup", select).apply()
        selectGroup = select
    }

    val isMasterPasswordCreated: Boolean
        get() = if (masterPassword != null) true else false

    fun getMasterPassword(): String? {
        return masterPassword
    }

    fun setMasterPassword(password: String?) {
        try {
            passUtil.saveMasterPasswordString("masterPassword", password)
        } catch (e: Exception) {
            Log.e(TAG, e.message, e)
        }
        updateSetting()
    }

    fun clearAllData() {
        sharedPref.edit().clear().apply()
        updateSetting()
    }

    private fun checkBackgroundColor(context: Context) {
        val backgroundColorUtil = BackgroundColorUtil(context, null)
        if (!backgroundColorUtil.isColorExists(sharedPref.getInt("backgroundColor", 0))) {
            sharedPref.edit().putInt("backgroundColor", context.resources.getColor(R.color.white))
                .commit()
        }
    }

    companion object {
        private var manager: LoginDataManager? = null
        private const val TAG = "LoginDataManager"
        private const val PREF_FILE_NAME = "com.highcom.LoginActivity.MasterPass"
        fun getInstance(activity: Activity): LoginDataManager? {
            if (manager == null) {
                manager = LoginDataManager(activity)
            }
            return manager
        }
    }
}