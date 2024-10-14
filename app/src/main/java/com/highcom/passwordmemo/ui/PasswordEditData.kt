package com.highcom.passwordmemo.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * パスワード編集データ
 *
 * @property edit 編集モード
 * @property id パスワードデータID
 * @property title タイトル
 * @property account アカウント
 * @property password パスワード
 * @property url サイトURL
 * @property groupId グループID
 * @property memo メモ
 * @property inputDate 更新日付
 */
@Parcelize
data class PasswordEditData(
    var edit: Boolean = false,
    var id: Long = 0,
    var title: String = "",
    val account: String = "",
    val password: String = "",
    val url: String = "",
    val groupId: Long = 1,
    val memo: String = "",
    val inputDate: String = "",
) : Parcelable