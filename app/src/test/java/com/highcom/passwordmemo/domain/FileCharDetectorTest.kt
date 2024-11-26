package com.highcom.passwordmemo.domain

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.highcom.passwordmemo.domain.file.FileCharDetector
import io.mockk.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.IOException
import org.mozilla.universalchardet.UniversalDetector
import org.robolectric.RobolectricTestRunner
import java.io.InputStream

/**
 * ファイルの文字コード判定クラスのUnitTest
 *
 */
@RunWith(RobolectricTestRunner::class)
class FileCharDetectorTest {

    private lateinit var mockContext: Context
    private lateinit var mockContentResolver: ContentResolver
    private lateinit var mockInputStream: InputStream
    private lateinit var fileCharDetector: FileCharDetector
    private lateinit var mockUri: Uri
    private lateinit var detector: UniversalDetector

    @Before
    fun setUp() {
        mockContext = mockk()
        mockContentResolver = mockk()
        mockUri = mockk()
        mockInputStream = mockk(relaxed = true)

        // モックした Uri で InputStream を返す設定
        every { mockContext.contentResolver } returns mockContentResolver
        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        // UniversalDetector をモック
        mockkConstructor(UniversalDetector::class)
        detector = mockk(relaxed = true)
        every { anyConstructed<UniversalDetector>().detectedCharset } returns "UTF-8"
        every { anyConstructed<UniversalDetector>().handleData(any(), any(), any()) } just Runs
        every { anyConstructed<UniversalDetector>().dataEnd() } just Runs

        // FileCharDetector のインスタンスを初期化
        fileCharDetector = FileCharDetector(mockContext, mockUri)
    }

    /**
     * 文字コード検出処理の成功テスト
     *
     */
    @Test
    fun detect_charset_success() {
        // 文字コード検出成功のシナリオ
        val expectedCharset = "UTF-8"

        val result = fileCharDetector.detect()

        assertEquals(expectedCharset, result)
        verify { anyConstructed<UniversalDetector>().dataEnd() }
    }

    /**
     * 文字コード検出処理の失敗テスト
     *
     */
    @Test
    fun detect_charset_fail() {
        every { anyConstructed<UniversalDetector>().detectedCharset } returns null

        val result = fileCharDetector.detect()

        assertNull(result)
        verify { anyConstructed<UniversalDetector>().dataEnd() }
    }

    /**
     * 文字コード検出処理のファイルオープン失敗テスト
     *
     */
    @Test(expected = IOException::class)
    fun detect_IOException() {
        // InputStream のオープン時に IOException が発生するシナリオをモック
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("File not found")

        fileCharDetector.detect()
    }
}