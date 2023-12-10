package com.highcom.passwordmemo.util.login

import android.util.Base64
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 暗号化/復号化のユーティリティ
 */
class CryptUtil {
    /**
     * 引数の文字列を暗号化する(Base64対応)
     *
     * @param value     暗号化対象文字列
     * @param secretKey 暗号化キー
     * @return String 暗号化済み文字列
     */
    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun encrypt(value: String, secretKey: String): String {
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(MODE)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, IvParameterSpec(IV.toByteArray()))
        val values = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(values, Base64.DEFAULT)
    }

    /**
     * 引数のBase64された文字列を復号化する
     *
     * @param value     復号化対象文字列
     * @param secretKey 復号化キー
     * @return String 復号化済み文字列
     */
    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class
    )
    fun decrypt(value: String?, secretKey: String): String {
        val values = Base64.decode(value, Base64.DEFAULT)
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), ALGORITHM)
        val cipher = Cipher.getInstance(MODE)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, IvParameterSpec(IV.toByteArray()))
        return String(cipher.doFinal(values))
    }

    companion object {
        // アルゴリズム
        private const val ALGORITHM = "Blowfish"
        private const val MODE = "Blowfish/CBC/PKCS5Padding"
        private const val IV = "abcdefgh" // Blowfishの場合は64ビット（8バイト）なので
    }
}