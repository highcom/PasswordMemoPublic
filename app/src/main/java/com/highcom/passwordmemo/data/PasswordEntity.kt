package com.highcom.passwordmemo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * パスワードデータエンティティ
 *
 * @property id パスワードデータID
 * @property title タイトル
 * @property account アカウント
 * @property password パスワード
 * @property url サイトURL
 * @property groupId グループID
 * @property memo メモ
 * @property inputDate 更新日付
 */
@Entity(tableName = "passworddata")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") var id: Long,
    @ColumnInfo(name = "title", defaultValue = "") var title: String,
    @ColumnInfo(name = "account", defaultValue = "") var account: String,
    @ColumnInfo(name = "password", defaultValue = "") var password: String,
    @ColumnInfo(name = "url", defaultValue = "") var url: String,
    @ColumnInfo(name = "group_id", defaultValue = "1") var groupId: Long,
    @ColumnInfo(name = "memo", defaultValue = "") var memo: String,
    @ColumnInfo(name = "inputdate", defaultValue = "") var inputDate: String,
    @ColumnInfo(name = "color", defaultValue = "0") var color: Int,
)
