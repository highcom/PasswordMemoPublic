package com.highcom.passwordmemo.domain

import android.content.SharedPreferences
import android.content.Context
import com.highcom.passwordmemo.domain.login.CryptUtil
import com.highcom.passwordmemo.domain.login.MasterPasswordUtil
import io.mockk.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import com.highcom.passwordmemo.R

/**
 * マスターパスワードに関するユーティリティクラスのUnitTest
 *
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MasterPasswordUtilTest {

    private lateinit var masterPasswordUtil: MasterPasswordUtil
    private lateinit var sharedPref: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private lateinit var cryptUtil: CryptUtil

    @Before
    fun setUp() {
        sharedPref = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        context = mockk(relaxed = true)

        every { context.getString(R.string.master_secret_key) } returns "your_mocked_secret_key"
        every { sharedPref.edit() } returns editor

        // CryptUtilのモック
        cryptUtil = mockk()
        every { cryptUtil.encrypt(any(), any()) } returns "encryptedPassword"
        every { cryptUtil.decrypt(any(), any()) } returns "decryptedPassword"

        // MasterPasswordUtilインスタンス作成
        masterPasswordUtil = MasterPasswordUtil(sharedPref, context)
    }

    /**
     * 暗号化したマスターパスワード保存処理の暗号化確認テスト
     *
     */
    @Test
    fun saveMasterPasswordString_encrypted_password() {
        val key = "masterPasswordKey"
        val value = "superSecretPassword"

        // メソッド実行
        try {
            masterPasswordUtil.saveMasterPasswordString(key, value)
        } catch (e: Exception) {
            fail("Exception should not be thrown: ${e.message}")
        }

        // SharedPreferencesに暗号化された値が保存されていることを検証
        verify { editor.putString(key, any()) }
    }

    /**
     * 複合化したマスターパスワード取得処理の複合化確認テスト
     *
     */
    @Test
    fun getMasterPasswordString_decrypted_password() {
        val key = "your_mocked_secret_key"
        val testPassword = "test_password"

        // 暗号化
        val cryptUtil = CryptUtil()
        val encryptedPassword = cryptUtil.encrypt(testPassword, key)
        // SharedPreferencesから暗号化された値を取得するようにモック
        every { sharedPref.getString(key, null) } returns encryptedPassword

        val result = masterPasswordUtil.getMasterPasswordString(key)

        // 結果が復号されたパスワードであることを検証
        assertEquals("test_password", result)
    }

    /**
     * 暗号化したマスターパスワード保存処理のkeyがnullの場合のExceptionテスト
     *
     */
    @Test(expected = Exception::class)
    fun saveMasterPasswordString_throw_exception_key_is_null() {
        masterPasswordUtil.saveMasterPasswordString(null, "password")
    }

    /**
     * 暗号化したマスターパスワード保存処理のvalueがnullの場合のExceptionテスト
     *
     */
    @Test(expected = Exception::class)
    fun saveMasterPasswordString_throw_exception_value_is_null() {
        masterPasswordUtil.saveMasterPasswordString("key", null)
    }

    /**
     * 複合化したマスターパスワード取得処理のkeyがnullの場合のExceptionテスト
     *
     */
    @Test(expected = Exception::class)
    fun getMasterPasswordString_throw_exception_key_is_null() {
        masterPasswordUtil.getMasterPasswordString(null)
    }
}