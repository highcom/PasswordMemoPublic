package com.highcom.passwordmemo.viewmodel
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.ui.viewmodel.GroupListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * グループ一覧ビューモデルのUnitTest
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GroupListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // テストで子ルーチンを制御するための設定
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: PasswordMemoRepository
    private lateinit var viewModel: GroupListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        // パラメータ取得のモッキング
        every { repository.groupList } returns flowOf(emptyList())
        // coroutineメソッドのモッキング
        coEvery { repository.insertGroup(any()) } returns Unit
        coEvery { repository.insertGroups(any()) } returns Unit
        coEvery { repository.updateGroup(any()) } returns Unit
        coEvery { repository.updateGroups(any()) } returns Unit
        coEvery { repository.deleteGroup(any()) } returns Unit
        coEvery { repository.resetGroupId(any()) } returns Unit
        viewModel = GroupListViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * グループ一覧データに値が設定されることのテスト
     *
     */
    @Test
    fun groupList_emits_data_correctly() = runTest {
        val testGroupList = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Group A"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Group B")
        )
        every { repository.groupList } returns flowOf(testGroupList)

        viewModel = GroupListViewModel(repository)

        val emittedData = mutableListOf<List<GroupEntity>>()
        viewModel.groupList.collect { emittedData.add(it) }

        assertEquals(1, emittedData.size)
        assertEquals(testGroupList, emittedData.first())
    }

    /**
     * グループデータ挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun insert_group_called_success() = runTest {
        val testGroup = GroupEntity(groupId = 1, groupOrder = 1, name = "Test Group")

        viewModel.insert(testGroup)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.insertGroup(testGroup) }
    }

    /**
     * グループデータ一覧の挿入処理の呼び出し成功テスト
     *
     */
    @Test
    fun insert_groups_called_success() = runTest {
        val testGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Group 1"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Group 2")
        )

        viewModel.insert(testGroups)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.insertGroups(testGroups) }
    }

    /**
     * グループデータ更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun update_group_called_success() = runTest {
        val testGroup = GroupEntity(groupId = 1, groupOrder = 1, name = "Updated Group")

        viewModel.update(testGroup)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.updateGroup(testGroup) }
    }

    /**
     * グループデータ一括更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun update_groups_called_success() = runTest {
        val testGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Updated Group 1"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Updated Group 2")
        )

        viewModel.update(testGroups)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.updateGroups(testGroups) }
    }

    /**
     * グループデータ削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun delete_group_called_success() = runTest {
        val groupIdToDelete = 1L

        viewModel.delete(groupIdToDelete)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.deleteGroup(groupIdToDelete) }
    }

    /**
     * 指定されたグループIDを処理グループIDにリセットする処理の呼び出し成功テスト
     *
     */
    @Test
    fun resetGroupId_called_success() = runTest {
        val groupIdToReset = 1L

        viewModel.resetGroupId(groupIdToReset)
        // 子ルーチンの処理を待つ
        advanceUntilIdle()

        coVerify { repository.resetGroupId(groupIdToReset) }
    }
}