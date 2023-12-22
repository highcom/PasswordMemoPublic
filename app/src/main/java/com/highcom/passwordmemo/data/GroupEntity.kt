package com.highcom.passwordmemo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * グループデータエンティティ
 *
 * @property groupId グループID
 * @property groupOrder 並び順
 * @property name グループ名
 */
@Entity(tableName = "groupdata")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "group_id") var groupId: Long,
    @ColumnInfo(name = "group_order") var groupOrder: Int,
    @ColumnInfo(name = "name", defaultValue = "") var name: String,
)
