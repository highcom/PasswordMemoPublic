package com.highcom.passwordmemo.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * パスワードメモデータアクセスリポジトリ
 *
 * DatabaseManager を注入し、DB インスタンスの再生成時に Flow を切り替える
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PasswordMemoRepository @Inject constructor(private val dbManager: DatabaseManager) {
    /**
     * グループ一覧データの Flow。
     *
     * DatabaseManager の再生成通知 ([DatabaseManager.dbRecreated]) をトリガーにして内部の DAO による Flow に切り替わる。
     * UI はこの Flow を購読するだけで、DB を置換した際に自動的に新しい DB のデータを受け取れる。
     */
    val groupList: Flow<List<GroupEntity>> =
        dbManager.dbRecreated
            .onStart { emit(Unit) }
            .flatMapLatest {
                dbManager.getDatabase().groupDao().getGroupList()
            }

    /**
     * パスワード一覧データ取得処理
     *
     * 指定したグループのパスワード一覧を Flow で返す。
     * DatabaseManager の再生成通知により内部の DAO による Flow に切り替わるため、
     * DB が置き換わっても購読を継続していれば自動的に新しい内容が流れてくる。
     *
     * @param groupId 取得対象のグループID（1L は全件）
     * @return パスワード一覧の Flow
     */
    fun getPasswordList(groupId: Long): Flow<List<PasswordEntity>> {
        return if (groupId == 1L) {
            dbManager.dbRecreated
                .onStart { emit(Unit) }
                .flatMapLatest { dbManager.getDatabase().passwordDao().getPasswordList() }
        } else {
            dbManager.dbRecreated
                .onStart { emit(Unit) }
                .flatMapLatest { dbManager.getDatabase().passwordDao().getSelectGroupPasswordList(groupId) }
        }
    }

    /**
     * パスワード総件数を同期的に取得する。
     *
     * @return パスワード総件数
     */
    suspend fun getPasswordCount(): Int {
        return dbManager.getDatabase().passwordDao().getPasswordCount()
    }

    /**
     * 単一のパスワードデータを挿入する。
     *
     * @param passwordEntity 挿入する [PasswordEntity]
     */
    suspend fun insertPassword(passwordEntity: PasswordEntity) {
        dbManager.getDatabase().passwordDao().insertPassword(passwordEntity)
    }

    /**
     * 複数のパスワードデータを一括挿入する。
     *
     * @param passwordList 挿入する [PasswordEntity] のリスト
     */
    suspend fun insertPasswords(passwordList: List<PasswordEntity>) {
        dbManager.getDatabase().passwordDao().insertPasswords(passwordList)
    }

    /**
     * 単一のパスワードデータを更新する。
     *
     * @param passwordEntity 更新対象の [PasswordEntity]
     */
    suspend fun updatePassword(passwordEntity: PasswordEntity) {
        dbManager.getDatabase().passwordDao().updatePassword(passwordEntity)
    }

    /**
     * 複数のパスワードデータを一括更新する。
     *
     * @param passwordList 更新対象の [PasswordEntity] リスト
     */
    suspend fun updatePasswords(passwordList: List<PasswordEntity>) {
        dbManager.getDatabase().passwordDao().updatePasswords(passwordList)
    }

    /**
     * 指定した ID のパスワードデータを削除する。
     *
     * @param id 削除するパスワードの ID
     */
    suspend fun deletePassword(id: Long) {
        dbManager.getDatabase().passwordDao().deletePassword(id)
    }

    /**
     * パスワードテーブルを全削除する。
     */
    suspend fun deleteAllPassword() {
        dbManager.getDatabase().passwordDao().deleteAllPassword()
    }

    /**
     * 指定されたグループID の参照を初期グループ（1）にリセットする。
     *
     * @param groupId リセット対象のグループID
     */
    suspend fun resetGroupId(groupId: Long) {
        dbManager.getDatabase().passwordDao().resetGroupId(groupId)
    }

    /**
     * グループを追加する。
     *
     * @param groupEntity 追加する [GroupEntity]
     */
    suspend fun insertGroup(groupEntity: GroupEntity) {
        dbManager.getDatabase().groupDao().insertGroup(groupEntity)
    }

    /**
     * グループを一括追加する。
     *
     * @param groupList 追加する [GroupEntity] のリスト
     */
    suspend fun insertGroups(groupList: List<GroupEntity>) {
        dbManager.getDatabase().groupDao().insertGroups(groupList)
    }

    /**
     * グループを更新する。
     *
     * @param groupEntity 更新対象の [GroupEntity]
     */
    suspend fun updateGroup(groupEntity: GroupEntity) {
        dbManager.getDatabase().groupDao().updateGroup(groupEntity)
    }

    /**
     * グループを一括更新する。
     *
     * @param groupList 更新対象の [GroupEntity] リスト
     */
    suspend fun updateGroups(groupList: List<GroupEntity>) {
        dbManager.getDatabase().groupDao().updateGroups(groupList)
    }

    /**
     * 指定したグループを削除する。
     *
     * @param groupId 削除対象のグループID
     */
    suspend fun deleteGroup(groupId: Long) {
        dbManager.getDatabase().groupDao().deleteGroup(groupId)
    }

    /**
     * グループテーブルを全削除する。
     */
    suspend fun deleteAllGroup() {
        dbManager.getDatabase().groupDao().deleteAllGroup()
    }
}