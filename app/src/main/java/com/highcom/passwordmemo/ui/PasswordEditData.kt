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
 * @property color 選択色
 */
@Parcelize
data class PasswordEditData(
    var edit: Boolean = false,
    var id: Long = 0,
    var title: String = "",
    var account: String = "",
    var password: String = "",
    var url: String = "",
    var groupId: Long = 1,
    var memo: String = "",
    var inputDate: String = "",
    var color: Int = 0,
) : Parcelable