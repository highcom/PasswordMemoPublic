package com.highcom.passwordmemo.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.ui.viewmodel.SettingViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * 設定画面ビューモデルのUnitTest
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // テストで子ルーチンを制御するための設定
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: PasswordMemoRepository
    private lateinit var viewModel: SettingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        // パラメータ取得のモッキング
        every { repository.getPasswordList(any()) } returns flowOf(emptyList())
        every { repository.groupList } returns flowOf(emptyList())
        viewModel = SettingViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * パスワード一覧データに値が設定されることのテスト
     *
     */
    @Test
    fun passwordList_emits_data_correctly() = runTest {
        val testPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111"),
            PasswordEntity(id = 1, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111"),
        )
        every { repository.getPasswordList(any()) } returns flowOf(testPasswords)

        viewModel = SettingViewModel(repository)

        val emittedData = mutableListOf<List<PasswordEntity>>()
        viewModel.passwordList.collect { emittedData.add(it) }

        TestCase.assertEquals(1, emittedData.size)
        TestCase.assertEquals(testPasswords, emittedData.first())
    }

    /**
     * グループ一覧データに値が設定されることのテスト
     *
     */
    @Test
    fun groupList_emits_data_correctly() = runTest {
        val testGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Group A"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Group B")
        )
        every { repository.groupList } returns flowOf(testGroups)

        viewModel = SettingViewModel(repository)

        val emittedData = mutableListOf<List<GroupEntity>>()
        viewModel.groupList.collect { emittedData.add(it) }

        TestCase.assertEquals(1, emittedData.size)
        TestCase.assertEquals(testGroups, emittedData.first())
    }

    /**
     * パスワードデータ一覧の再挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun reInsertPassword_called_success() = runTest {
        val testPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111"),
            PasswordEntity(id = 1, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111"),
        )
        coEvery { repository.deleteAllPassword() } returns Unit
        coEvery { repository.insertPasswords(any()) } returns Unit

        viewModel.reInsertPassword(testPasswords)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.deleteAllPassword() }
        coVerify { repository.insertPasswords(testPasswords) }
    }

    /**
     * グループデータ一覧の再挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun reInsertGroup_called_success() = runTest {
        val testGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Group A"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Group B")
        )
        coEvery { repository.deleteAllGroup() } returns Unit
        coEvery { repository.insertGroups(any()) } returns Unit

        viewModel.reInsertGroup(testGroups)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.deleteAllGroup() }
        coVerify { repository.insertGroups(testGroups) }
    }
}