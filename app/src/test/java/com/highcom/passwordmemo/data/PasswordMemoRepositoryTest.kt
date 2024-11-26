package com.highcom.passwordmemo.data

import kotlinx.coroutines.flow.flowOf
import io.mockk.*
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.test.runTest
import junit.framework.TestCase.assertEquals

/**
 * パスワードメモデータアクセスリポジトリのUnitTest
 *
 */
class PasswordMemoRepositoryTest {

    private lateinit var passwordDao: PasswordDao
    private lateinit var groupDao: GroupDao
    private lateinit var repository: PasswordMemoRepository

    @Before
    fun setUp() {
        passwordDao = mockk()
        groupDao = mockk()
        every { groupDao.getGroupList() } returns flowOf(emptyList())
        repository = PasswordMemoRepository(passwordDao, groupDao)
    }

    /**
     * グループ一覧データに値が設定されることのテスト
     *
     */
    @Test
    fun groupList_emits_data_correctly() = runTest {
        val expectedGroupList = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Group A"),
            GroupEntity(groupId = 2, groupOrder = 2, name = "Group B")
        )
        every { groupDao.getGroupList() } returns flowOf(expectedGroupList)
        repository = PasswordMemoRepository(passwordDao, groupDao)

        val result = repository.groupList

        result.collect { groupList ->
            assertEquals(expectedGroupList, groupList)
        }
        verify { groupDao.getGroupList() }
    }

    /**
     * パスワード一覧データ取得処理にすべての値が設定されることのテスト
     *
     */
    @Test
    fun getPasswordList_emits_all_passwords() = runTest {
        val expectedPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111"),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111"),
        )
        every { passwordDao.getPasswordList() } returns flowOf(expectedPasswords)
        repository = PasswordMemoRepository(passwordDao, groupDao)

        val result = repository.getPasswordList(1L)

        result.collect { passwordList ->
            assertEquals(expectedPasswords, passwordList)
        }
        verify { passwordDao.getPasswordList() }
    }

    /**
     * パスワード一覧データ取得処理にフォルたされた値が設定されることのテスト
     *
     */
    @Test
    fun getPasswordList_filtered_passwords() = runTest {
        val groupId = 2L
        val expectedPasswords = listOf(PasswordEntity(id = 3, title = "c", account = "c", password = "c", url = "test", groupId = 2, memo = "c", inputDate = "20241111"))
        every { passwordDao.getSelectGroupPasswordList(groupId) } returns flowOf(expectedPasswords)
        repository = PasswordMemoRepository(passwordDao, groupDao)

        val result = repository.getPasswordList(groupId)

        result.collect { passwordList ->
            assertEquals(expectedPasswords, passwordList)
        }
        verify { passwordDao.getSelectGroupPasswordList(groupId) }
    }

    /**
     * パスワードデータ追加処理の呼び出し成功テスト
     *
     */
    @Test
    fun insertPassword_called_success() = runTest {
        val password = PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111")
        coEvery { passwordDao.insertPassword(password) } just Runs

        repository.insertPassword(password)

        coVerify { passwordDao.insertPassword(password) }
    }

    /**
     * パスワードデータ一括追加処理の呼び出し成功テスト
     *
     */
    @Test
    fun insertPasswords_called_success() = runTest {
        val expectedPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111"),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111"),
        )
        coEvery { passwordDao.insertPasswords(expectedPasswords) } just Runs

        repository.insertPasswords(expectedPasswords)

        coVerify { passwordDao.insertPasswords(expectedPasswords) }
    }

    /**
     * パスワードデータ更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun updatePassword_called_success() = runTest {
        val password = PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111")
        coEvery { passwordDao.updatePassword(password) } just Runs

        repository.updatePassword(password)

        coVerify { passwordDao.updatePassword(password) }
    }

    /**
     * パスワードデータ一括更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun updatePasswords_called_success() = runTest {
        val expectedPasswords = listOf(
            PasswordEntity(id = 1, title = "a", account = "a", password = "a", url = "test", groupId = 1, memo = "a", inputDate = "20241111"),
            PasswordEntity(id = 2, title = "b", account = "b", password = "b", url = "test", groupId = 1, memo = "b", inputDate = "20241111"),
        )
        coEvery { passwordDao.updatePasswords(expectedPasswords) } just Runs

        repository.updatePasswords(expectedPasswords)

        coVerify { passwordDao.updatePasswords(expectedPasswords) }
    }

    /**
     * パスワードデータ削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun deletePassword_called_success() = runTest {
        val passwordId = 1L
        coEvery { passwordDao.deletePassword(passwordId) } just Runs

        repository.deletePassword(passwordId)

        coVerify { passwordDao.deletePassword(passwordId) }
    }

    /**
     * パスワードデータ全削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun deleteAllPassword_called_success() = runTest {
        val passwordId = 1L
        coEvery { passwordDao.deleteAllPassword() } just Runs

        repository.deleteAllPassword()

        coVerify { passwordDao.deleteAllPassword() }
    }

    /**
     * 指定されたグループIDを初期グループIDにリセットする処理の呼び出し成功テスト
     *
     */
    @Test
    fun resetGroupId_called_success() = runTest {
        val groupId = 1L
        coEvery { passwordDao.resetGroupId(groupId) } just Runs

        repository.resetGroupId(groupId)

        coVerify { passwordDao.resetGroupId(groupId) }
    }

    /**
     * グループデータ追加処理の呼び出し成功テスト
     *
     */
    @Test
    fun insertGroup_called_success() = runTest {
        val group = GroupEntity(groupId = 1, groupOrder = 1, name = "Test Group")
        coEvery { groupDao.insertGroup(group) } just Runs

        repository.insertGroup(group)

        coVerify { groupDao.insertGroup(group) }
    }

    /**
     * グループデータ一括追加処理の呼び出し成功テスト
     *
     */
    @Test
    fun insertGroups_called_success() = runTest {
        val expectedGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Test Group"),
            GroupEntity(groupId = 1, groupOrder = 2, name = "Test Group2")
        )
        coEvery { groupDao.insertGroups(expectedGroups) } just Runs

        repository.insertGroups(expectedGroups)

        coVerify { groupDao.insertGroups(expectedGroups) }
    }

    /**
     * グループデータ更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun updateGroup_called_success() = runTest {
        val group = GroupEntity(groupId = 1, groupOrder = 1, name = "Test Group")
        coEvery { groupDao.updateGroup(group) } just Runs

        repository.updateGroup(group)

        coVerify { groupDao.updateGroup(group) }
    }

    /**
     * グループデータ一括更新処理の呼び出し成功テスト
     *
     */
    @Test
    fun updateGroups_called_success() = runTest {
        val expectedGroups = listOf(
            GroupEntity(groupId = 1, groupOrder = 1, name = "Test Group"),
            GroupEntity(groupId = 1, groupOrder = 2, name = "Test Group2")
        )
        coEvery { groupDao.updateGroups(expectedGroups) } just Runs

        repository.updateGroups(expectedGroups)

        coVerify { groupDao.updateGroups(expectedGroups) }
    }

    /**
     * グループデータ削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun deleteGroup_called_success() = runTest {
        val groupId = 2L
        coEvery { groupDao.deleteGroup(groupId) } just Runs

        repository.deleteGroup(groupId)

        coVerify { groupDao.deleteGroup(groupId) }
    }

    /**
     * グループデータ一括削除処理の呼び出し成功テスト
     *
     */
    @Test
    fun deleteAllGroup_called_success() = runTest {
        coEvery { groupDao.deleteAllGroup() } just Runs

        repository.deleteAllGroup()

        coVerify { groupDao.deleteAllGroup() }
    }
}