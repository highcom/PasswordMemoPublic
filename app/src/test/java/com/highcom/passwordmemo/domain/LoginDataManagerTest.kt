package com.highcom.passwordmemo.domain

import android.app.Application
import android.content.SharedPreferences
import com.highcom.passwordmemo.domain.login.LoginDataManager
import com.highcom.passwordmemo.domain.login.MasterPasswordUtil
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ログインデータ管理クラスのUnitTest
 *
 */
@RunWith(RobolectricTestRunner::class)
class LoginDataManagerTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var application: Application
    private lateinit var loginDataManager: LoginDataManager

    @Before
    fun setUp() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        application = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences

        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putFloat(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.clear() } returns editor
        every { editor.apply() } just Runs

        loginDataManager = LoginDataManager(application)
    }

    /**
     * 設定値の更新処理が設定されることのテスト
     *
     */
    @Test
    fun updateSetting_values_correctly() {
        every { sharedPreferences.getString("sortKey", "id") } returns "name"
        every { sharedPreferences.getBoolean("deleteSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("biometricLoginSwitchEnable", true) } returns false
        every { sharedPreferences.getBoolean("displayBackgroundSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("memoVisibleSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("passwordVisibleSwitchEnable", false) } returns true
        every { sharedPreferences.getInt("backgroundColor", 0) } returns 12345
        every { sharedPreferences.getFloat("textSize", TextSizeUtil.TEXT_SIZE_MEDIUM.toFloat()) } returns 10F
        every { sharedPreferences.getInt("copyClipboard", 0) } returns 1
        every { sharedPreferences.getLong("selectGroup", 1) } returns 2

        loginDataManager.updateSetting()

        assertEquals("name", loginDataManager.sortKey)
        assertTrue(loginDataManager.deleteSwitchEnable)
        assertFalse(loginDataManager.biometricLoginSwitchEnable)
        assertTrue(loginDataManager.displayBackgroundSwitchEnable)
        assertTrue(loginDataManager.memoVisibleSwitchEnable)
        assertTrue(loginDataManager.passwordVisibleSwitchEnable)
        assertEquals(12345, loginDataManager.backgroundColor)
        assertEquals(10F, loginDataManager.textSize)
        assertEquals(1, loginDataManager.copyClipboard)
        assertEquals(2, loginDataManager.selectGroup)
    }

    /**
     * パスワード一覧のソートキー設定処理でsortKeyが設定されることのテスト
     *
     */
    @Test
    fun setSortKey_updates_sortKey() {
        loginDataManager.setSortKey("name")

        verify { editor.putString("sortKey", "name") }
        verify { editor.apply() }
        assertEquals("name", loginDataManager.sortKey)
    }

    /**
     * 全データ削除スイッチの設定処理でdeleteSwitchEnableが設定されることのテスト
     *
     */
    @Test
    fun setDeleteSwitchEnable_updates_deleteSwitchEnable() {
        every { sharedPreferences.getBoolean("deleteSwitchEnable", false) } returns true
        loginDataManager.setDeleteSwitchEnable(true)

        verify { editor.putBoolean("deleteSwitchEnable", true) }
        verify { editor.apply() }
        assertTrue(loginDataManager.deleteSwitchEnable)
    }

    /**
     * 生体認証ログインスイッチの設定処理でbiometricLoginSwitchEnableが設定されることのテスト
     *
     */
    @Test
    fun setBiometricLoginSwitchEnable_updates_biometricLoginSwitchEnable() {
        every { sharedPreferences.getBoolean("biometricLoginSwitchEnable", true) } returns false
        loginDataManager.setBiometricLoginSwitchEnable(false)

        verify { editor.putBoolean("biometricLoginSwitchEnable", false) }
        verify { editor.apply() }
        assertFalse(loginDataManager.biometricLoginSwitchEnable)
    }

    /**
     * バックグラウンド時の非表示スイッチの設定処理でdisplayBackgroundSwitchEnableが設定されることのテスト
     *
     */
    @Test
    fun setDisplayBackgroundSwitchEnable_updates_displayBackgroundSwitchEnable() {
        every { sharedPreferences.getBoolean("displayBackgroundSwitchEnable", false) } returns true
        loginDataManager.setDisplayBackgroundSwitchEnable(true)

        verify { editor.putBoolean("displayBackgroundSwitchEnable", true) }
        verify { editor.apply() }
        assertTrue(loginDataManager.displayBackgroundSwitchEnable)
    }

    /**
     * パスワード一覧にメモの表示設定処理でmemoVisibleSwitchEnableが設定されることのテスト
     *
     */
    @Test
    fun setMemoVisibleSwitchEnable_updates_memoVisibleSwitchEnable() {
        every { sharedPreferences.getBoolean("memoVisibleSwitchEnable", false) } returns true
        loginDataManager.setMemoVisibleSwitchEnable(true)

        verify { editor.putBoolean("memoVisibleSwitchEnable", true) }
        verify { editor.apply() }
        assertTrue(loginDataManager.memoVisibleSwitchEnable)
    }

    /**
     * 参照画面でのパスワード欄の初期表示設定処理でpasswordVisibleSwitchEnableが設定されることのテスト
     *
     */
    @Test
    fun setPasswordVisibleSwitchEnable_updates_passwordVisibleSwitchEnable() {
        every { sharedPreferences.getBoolean("passwordVisibleSwitchEnable", false) } returns true
        loginDataManager.setPasswordVisibleSwitchEnable(true)

        verify { editor.putBoolean("passwordVisibleSwitchEnable", true) }
        verify { editor.apply() }
        assertTrue(loginDataManager.passwordVisibleSwitchEnable)
    }

    /**
     * 背景色設定処理でbackgroundColorが設定されることのテスト
     *
     */
    @Test
    fun setBackgroundColor_updates_backgroundColor() {
        loginDataManager.setBackgroundColor(12345)

        verify { editor.putInt("backgroundColor", 12345) }
        verify { editor.apply() }
        assertEquals(12345, loginDataManager.backgroundColor)
    }

    /**
     * テキストサイズ設定処理でtextSizeが設定されることのテスト
     *
     */
    @Test
    fun setTextSize_updates_textSize() {
        loginDataManager.setTextSize(18f)

        verify { editor.putFloat("textSize", 18f) }
        verify { editor.apply() }
        assertEquals(18f, loginDataManager.textSize)
    }

    /**
     * クリップボードにコピーする方法の設定処理でcopyClipboardが設定されることのテスト
     *
     */
    @Test
    fun setCopyClipboard_updates_copyClipboard() {
        loginDataManager.setCopyClipboard(1)

        verify { editor.putInt("copyClipboard", 1) }
        verify { editor.apply() }
        assertEquals(1, loginDataManager.copyClipboard)
    }

    /**
     * 選択グループ設定処理でselectGroupが設定されることのテスト
     *
     */
    @Test
    fun setSelectGroup_updates_selectGroup() {
        loginDataManager.setSelectGroup(1L)

        verify { editor.putLong("selectGroup", 1L) }
        verify { editor.apply() }
        assertEquals(1L, loginDataManager.selectGroup)
    }

    /**
     * マスターパスワード設定処理でmasterPasswordが設定されることのテスト
     *
     */
    @Test
    fun setMasterPassword_updates_masterPassword() {
        val masterPasswordUtil = mockk<MasterPasswordUtil>(relaxed = true)
        every { application.getSharedPreferences(any(), any()) } returns sharedPreferences
        every { masterPasswordUtil.getMasterPasswordString(any()) } returns "securePassword"

        mockkConstructor(MasterPasswordUtil::class)
        every { anyConstructed<MasterPasswordUtil>().saveMasterPasswordString(any(), any()) } just Runs
        loginDataManager = LoginDataManager(application)

        loginDataManager.setMasterPassword("securePassword")

        verify { anyConstructed<MasterPasswordUtil>().saveMasterPasswordString("masterPassword", "securePassword") }
    }

    /**
     * 全データ削除処理で全てのデータが削除されて初期値にリセットされることのテスト
     *
     */
    @Test
    fun clearAllData_clears_SharedPreferences_resets_values() {
        every { sharedPreferences.getString("sortKey", "id") } returns "name"
        every { sharedPreferences.getBoolean("deleteSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("biometricLoginSwitchEnable", true) } returns false
        every { sharedPreferences.getBoolean("displayBackgroundSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("memoVisibleSwitchEnable", false) } returns true
        every { sharedPreferences.getBoolean("passwordVisibleSwitchEnable", false) } returns true
        every { sharedPreferences.getInt("backgroundColor", 0) } returns 12345
        every { sharedPreferences.getFloat("textSize", TextSizeUtil.TEXT_SIZE_MEDIUM.toFloat()) } returns 10F
        every { sharedPreferences.getInt("copyClipboard", 0) } returns 1
        every { sharedPreferences.getLong("selectGroup", 1) } returns 2

        loginDataManager.clearAllData()

        verify { editor.clear() }
        verify { editor.apply() }
        assertEquals("name", loginDataManager.sortKey)
        assertTrue(loginDataManager.deleteSwitchEnable)
        assertFalse(loginDataManager.biometricLoginSwitchEnable)
        assertTrue(loginDataManager.displayBackgroundSwitchEnable)
        assertTrue(loginDataManager.memoVisibleSwitchEnable)
        assertTrue(loginDataManager.passwordVisibleSwitchEnable)
        assertEquals(12345, loginDataManager.backgroundColor)
        assertEquals(10F, loginDataManager.textSize)
        assertEquals(1, loginDataManager.copyClipboard)
        assertEquals(2, loginDataManager.selectGroup)
    }
}