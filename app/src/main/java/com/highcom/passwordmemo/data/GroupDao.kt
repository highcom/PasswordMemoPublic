package com.highcom.passwordmemo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    /**
     * グループデータ一覧取得
     *
     * @return グループデータ
     */
    @Query("SELECT * FROM groupdata ORDER BY group_id ASC")
    fun getGroupList(): Flow<List<GroupEntity>>

    /**
     * グループデータ追加
     *
     * @param groupEntity グループデータ
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(groupEntity: GroupEntity)

    /**
     * グループデータ更新
     *
     * @param groupEntity グループデータ
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGroup(groupEntity: GroupEntity)

    /**
     * グループデータ一括更新
     *
     * @param groupList グループデータリスト
     */
    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateGroups(groupList: List<GroupEntity>)

    /**
     * グループデータ削除
     *
     * @param id グループID
     */
    @Query("DELETE FROM groupdata WHERE group_id = :groupId")
    suspend fun deleteGroup(groupId: Long)
}