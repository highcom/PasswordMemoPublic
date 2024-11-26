package com.highcom.passwordmemo.domain

import com.highcom.passwordmemo.domain.login.CryptUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.IllegalArgumentException
import javax.crypto.BadPaddingException

/**
 * 暗号化/復号化のユーティリティのUnitTest
 *
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28]) // 必要な API レベルを指定
class CryptUtilTest {

    private val cryptUtil = CryptUtil()
    private val secretKey = "supersecretkey" // 16文字未満のキーを使用する場合は適宜変更してください
    private val testString = "Hello, World!"

    /**
     * 暗号化/複合化の成功確認テスト
     *
     */
    @Test
    fun encryptDecrypt_Success() {
        // 暗号化
        val encrypted = cryptUtil.encrypt(testString, secretKey)
        // 復号化
        val decrypted = cryptUtil.decrypt(encrypted, secretKey)

        // 元の文字列と復号化された文字列が一致することを確認
        assertEquals(testString, decrypted)
    }

    /**
     * 無効キーによる複合化のエラーテスト
     *
     */
    @Test
    fun decrypt_InvalidKey_ThrowsException() {
        // 暗号化
        val encrypted = cryptUtil.encrypt(testString, secretKey)
        val invalidKey = "invalidkey"

        // 無効なキーを使用して復号化すると例外がスローされることを確認
        assertThrows(BadPaddingException::class.java) {
            cryptUtil.decrypt(encrypted, invalidKey)
        }
    }

    /**
     * 無効なBase64文字列による複合化のエラーテスト
     *
     */
    @Test
    fun decrypt_InvalidBase64_ThrowsException() {
        val invalidBase64 = "invalid_base64"

        // 無効なBase64文字列を復号化しようとすると例外がスローされることを確認
        assertThrows(IllegalArgumentException::class.java) {
            cryptUtil.decrypt(invalidBase64, secretKey)
        }
    }
}