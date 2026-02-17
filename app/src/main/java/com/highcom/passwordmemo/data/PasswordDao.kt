package com.highcom.passwordmemo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * パスワードデータアクセスオブジェクト
 *
 */
@Dao
interface PasswordDao {
    /**
     * パスワードデータ一覧取得
     *
     * @return パスワードデータ
     */
    @Query("SELECT * FROM passworddata ORDER BY id ASC")
    fun getPasswordList(): Flow<List<PasswordEntity>>

    /**
     * オートフィル用に全パスワードデータを一括取得
     *
     * @return パスワードデータ一覧
     */
    @Query("SELECT * FROM passworddata ORDER BY id ASC")
    suspend fun getAllPasswords(): List<PasswordEntity>

    /**
     * 選択グループのパスワードデータ一覧取得
     *
     * @return 選択グループのパスワードデータ
     */
    @Query("SELECT * FROM passworddata WHERE group_id = :groupId ORDER BY id ASC")
    fun getSelectGroupPasswordList(groupId: Long): Flow<List<PasswordEntity>>

    /**
     * パスワードデータの総件数を取得
     */
    @Query("SELECT COUNT(*) FROM passworddata")
    suspend fun getPasswordCount(): Int

    /**
     * パスワードデータ追加
     *
     * @param passwordEntity パスワードデータ
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPassword(passwordEntity: PasswordEntity)

    /**
     * パスワードデータ一括追加
     *
     * @param passwordList パスワードデータリスト
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPasswords(passwordList: List<PasswordEntity>)

    /**
     * パスワードデータ更新
     *
     * @param passwordEntity パスワードデータ
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePassword(passwordEntity: PasswordEntity)

    /**
     * パスワードデータ一括更新
     *
     * @param passwordList パスワードデータリスト
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePasswords(passwordList: List<PasswordEntity>)

    /**
     * パスワードデータ削除
     *
     * @param id パスワードID
     */
    @Query("DELETE FROM passworddata WHERE id = :id")
    suspend fun deletePassword(id: Long)

    /**
     * パスワードデータ全削除
     *
     */
    @Query("DELETE FROM passworddata")
    suspend fun deleteAllPassword()

    /**
     * 指定されたグループIDを初期グループIDにリセットする
     *
     * @param groupId リセットするグループID
     */
    @Query("UPDATE passworddata SET group_id=1 WHERE group_id = :groupId")
    suspend fun resetGroupId(groupId: Long)
}