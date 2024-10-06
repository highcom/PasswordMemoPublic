package com.highcom.passwordmemo.ui

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PasswordEditData(
    val edit: Boolean = false,
    val id: Long = 0,
    val title: String = "",
    val account: String = "",
    val password: String = "",
    val url: String = "",
    val groupId: Long = 1,
    val memo: String = "",
    val inputDate: String = "",
) : Parcelable