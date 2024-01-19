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
     * パスワードデータ追加
     *
     * @param passwordEntity パスワードデータ
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPassword(passwordEntity: PasswordEntity)

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
}