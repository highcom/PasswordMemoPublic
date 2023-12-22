package com.highcom.passwordmemo.data

import kotlinx.coroutines.flow.Flow

/**
 * パスワードメモデータアクセスリポジトリ
 *
 * @property passwordDao パスワードデータアクセスオブジェクト
 * @property groupDao グループデータアクセスオブジェクト
 */
class PasswordMemoRepository(private val passwordDao: PasswordDao, private val groupDao: GroupDao) {
    /** パスワード一覧データ */
    val passwordList: Flow<List<PasswordEntity>> = passwordDao.getPasswordList()
    /** グループ一覧データ */
    val groupList: Flow<List<GroupEntity>> = groupDao.getGroupList()

    /**
     * パスワードデータ追加
     *
     * @param passwordEntity パスワードデータ
     */
    suspend fun insertPassword(passwordEntity: PasswordEntity) {
        passwordDao.insertPassword(passwordEntity)
    }

    /**
     * パスワードデータ更新
     *
     * @param passwordEntity パスワードデータ
     */
    suspend fun updatePassword(passwordEntity: PasswordEntity) {
        passwordDao.updatePassword(passwordEntity)
    }

    /**
     * パスワードデータ一括更新
     *
     * @param passwordList パスワードデータリスト
     */
    suspend fun updatePasswords(passwordList: List<PasswordEntity>) {
        passwordDao.updatePasswords(passwordList)
    }

    /**
     * パスワードデータ削除
     *
     * @param id パスワードID
     */
    suspend fun deletePassword(id: Long) {
        passwordDao.deletePassword(id)
    }

    /**
     * グループデータ追加
     *
     * @param groupEntity グループデータ
     */
    suspend fun insertGroup(groupEntity: GroupEntity) {
        groupDao.insertGroup(groupEntity)
    }

    /**
     * グループデータ更新
     *
     * @param groupEntity グループデータ
     */
    suspend fun updateGroup(groupEntity: GroupEntity) {
        groupDao.updateGroup(groupEntity)
    }

    /**
     * グループデータ一括更新
     *
     * @param groupList グループデータリスト
     */
    suspend fun updateGroups(groupList: List<GroupEntity>) {
        groupDao.updateGroups(groupList)
    }

    /**
     * グループデータ削除
     *
     * @param id グループID
     */
    suspend fun deleteGroup(groupId: Long) {
        groupDao.deleteGroup(groupId)
    }
}