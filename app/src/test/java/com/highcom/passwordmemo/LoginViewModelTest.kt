package com.highcom.passwordmemo

import android.content.Context
import android.os.Handler
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.ui.viewmodel.LoginViewModel
import com.highcom.passwordmemo.util.login.LoginDataManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * ログイン画面ビューモデルのUnitTest
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: PasswordMemoRepository
    private lateinit var loginDataManager: LoginDataManager
    private lateinit var viewModel: LoginViewModel
    private lateinit var handler: Handler
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        loginDataManager = mockk(relaxed = true)
        handler = mockk(relaxed = true)
        viewModel = LoginViewModel(repository, loginDataManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 案内メッセージ初期化処理のメッセージの設定確認テスト
     *
     */
    @Test
    fun resetNaviMessage_message_correct() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.isMasterPasswordCreated } returns true
        every { context.getString(R.string.input_password) } returns "Enter password"
        every { context.getString(R.string.new_password) } returns "Create new password"

        viewModel.resetNaviMessage(context)

        assertEquals("Enter password", viewModel.naviMessage.value)

        every { loginDataManager.isMasterPasswordCreated } returns false
        viewModel.resetNaviMessage(context)

        assertEquals("Create new password", viewModel.naviMessage.value)
    }

    /**
     * パスワードログイン判定メッセージ取得処理の初期パスワードが設定されることのテスト
     *
     */
    @Test
    fun passwordLogin_master_password_creation_success() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.isMasterPasswordCreated } returns false
        every { context.getString(R.string.err_empty) } returns "Password is empty"
        every { context.getString(R.string.err_input_same) } returns "Enter the same password again"
        every { context.getString(R.string.login_success) } returns "Logging in..."

        viewModel.editMasterPassword.value = ""
        viewModel.passwordLogin(context)

        assertEquals("Password is empty", viewModel.naviMessage.value)

        // 初回パスワード入力
        viewModel.editMasterPassword.value = "testPassword"
        viewModel.passwordLogin(context)

        assertEquals("Enter the same password again", viewModel.naviMessage.value)

        // 2回目パスワード入力
        viewModel.editMasterPassword.value = "testPassword"
        viewModel.passwordLogin(context)

        // Then
        verify { loginDataManager.setMasterPassword("testPassword") }
        assertEquals("Logging in...", viewModel.naviMessage.value)
        assertTrue(viewModel.keyIconRotate.value)
    }

    /**
     * パスワードログイン判定メッセージ取得処理の初期パスワードの設定失敗のテスト
     *
     */
    @Test
    fun passwordLogin_master_password_creation_failure() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.isMasterPasswordCreated } returns false
        every { context.getString(R.string.err_empty) } returns "Password is empty"
        every { context.getString(R.string.err_input_same) } returns "Enter the same password again"
        every { context.getString(R.string.new_password) } returns "Create a new password"

        viewModel.editMasterPassword.value = ""
        viewModel.passwordLogin(context)

        assertEquals("Password is empty", viewModel.naviMessage.value)

        // 初回パスワード入力
        viewModel.editMasterPassword.value = "testPassword"
        viewModel.passwordLogin(context)

        // Then
        assertEquals("Enter the same password again", viewModel.naviMessage.value)

        // 2回目パスワード入力(異なるパスワード)
        viewModel.editMasterPassword.value = "wrongPassword"
        viewModel.passwordLogin(context)

        assertEquals("Create a new password", viewModel.naviMessage.value)
        assertFalse(viewModel.keyIconRotate.value)
    }

    /**
     * パスワードログイン判定メッセージ取得処理のログイン成功時のテスト
     *
     */
    @Test
    fun passwordLogin_enter_correct_password() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.masterPassword } returns "testPassword"
        every { loginDataManager.isMasterPasswordCreated } returns true
        every { loginDataManager.deleteSwitchEnable } returns false
        every { context.getString(R.string.login_success) } returns "Logging in..."

        viewModel.editMasterPassword.value = "testPassword"
        viewModel.passwordLogin(context)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()
        assertEquals("Logging in...", viewModel.naviMessage.value)
        assertTrue(viewModel.keyIconRotate.value)
    }

    /**
     * パスワードログイン判定メッセージ取得処理のログイン失敗時のテスト
     *
     */
    @Test
    fun passwordLogin_enter_wrong_password() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.isMasterPasswordCreated } returns true
        every { loginDataManager.deleteSwitchEnable } returns false
        every { context.getString(R.string.err_incorrect) } returns "Incorrect password"

        viewModel.editMasterPassword.value = "wrongPassword"
        viewModel.passwordLogin(context)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()
        assertEquals("Incorrect password", viewModel.naviMessage.value)
        assertFalse(viewModel.keyIconRotate.value)
    }

    /**
     * パスワードログイン判定メッセージ取得処理のパスワード誤りによる全データ消去テスト
     *
     */
    @Test
    fun passwordLogin_clear_all_data() = runTest {
        val context = mockk<Context>(relaxed = true)
        every { loginDataManager.isMasterPasswordCreated } returns true
        every { loginDataManager.deleteSwitchEnable } returns true
        every { context.getString(R.string.new_password) } returns "Create new password"

        // 5回パスワードを間違える
        for (i in 1..5) {
            viewModel.editMasterPassword.value = "wrongPassword"
            viewModel.passwordLogin(context)
        }
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        verify { loginDataManager.clearAllData() }
        coVerify { repository.deleteAllPassword() }
        coVerify { repository.deleteAllGroup() }
        assertEquals("Create new password", viewModel.naviMessage.value)
    }
}