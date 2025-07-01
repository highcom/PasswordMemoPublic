package com.highcom.passwordmemo.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.ui.viewmodel.PasswordListViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * パスワード一覧ビューモデルのUnitTest
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordListViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: PasswordListViewModel
    private lateinit var repository: PasswordMemoRepository
    // テストで子ルーチンを制御するための設定
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        // リポジトリのモッキング
        repository = mockk()
        coEvery { repository.insertPassword(any()) } returns Unit
        coEvery { repository.insertPasswords(any()) } returns Unit
        coEvery { repository.updatePassword(any()) } returns Unit
        coEvery { repository.updatePasswords(any()) } returns Unit
        coEvery { repository.deletePassword(any()) } returns Unit
        viewModel = PasswordListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * 選択グループ設定処理が呼ばれた時にパスワード一覧が設定されることのテスト
     *
     */
    @Test
    fun passwordList_emits_data_correctly() = runTest {
        // モックデータを準備
        val groupId = 1L
        val expectedPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = groupId, memo = "a", inputDate = "20241111", color = 0),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = groupId, memo = "b", inputDate = "20241111", color = 0),
        )
        val mockFlow: Flow<List<PasswordEntity>> = flow { emit(expectedPasswords) }

        coEvery { repository.getPasswordList(any()) } returns mockFlow

        // ViewModel に groupId を設定
        viewModel.setSelectGroup(groupId)

        // Flow のデータを確認
        val result = mutableListOf<List<PasswordEntity>>()
        val job = launch {
            viewModel.passwordList.toList(result)
        }

        advanceUntilIdle()
        job.cancel()

        assertEquals(expectedPasswords, result.first())
    }

    /**
     * パスワードデータ挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun insert_password_called_success() = runTest {
        val passwordEntity = PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111", color = 0)

        viewModel.insert(passwordEntity)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.insertPassword(passwordEntity) }
    }

    /**
     * パスワードデータ一覧の挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun insert_passwords_called_success() = runTest {
        val testPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111", color = 0),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111", color = 0),
        )

        viewModel.insert(testPasswords)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.insertPasswords(testPasswords) }
    }

    /**
     * パスワードデータ更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun update_password_called_success() = runTest {
        val passwordEntity = PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111", color = 0)

        viewModel.update(passwordEntity)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.updatePassword(passwordEntity) }
    }

    /**
     * パスワードデータ一覧の更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun update_passwords_called_success() = runTest {
        val testPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111", color = 0),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111", color = 0),
        )

        viewModel.update(testPasswords)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.updatePasswords(testPasswords) }
    }

    /**
     * パスワードデータ削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun delete_password_called_success() = runTest {
        val id = 1L

        viewModel.delete(id)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.deletePassword(id) }
    }
}