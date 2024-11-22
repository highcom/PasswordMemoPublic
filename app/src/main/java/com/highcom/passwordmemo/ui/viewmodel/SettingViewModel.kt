package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 設定画面ビューモデル
 *
 * @property repository データアクセスリポジトリ
 */
@HiltViewModel
class SettingViewModel @Inject constructor(private val repository: PasswordMemoRepository) : ViewModel() {
    /** パスワード一覧 */
    val passwordList = repository.getPasswordList(1L)
    /** グループ一覧 */
    val groupList = repository.groupList

    /**
     * パスワードデータ一覧の再挿入
     * * 既存のパスワード一覧を削除して再度パスワード一覧データを挿入する
     *
     * @param passwordList 再挿入するパスワード一覧
     */
    fun reInsertPassword(passwordList: List<PasswordEntity>) = viewModelScope.launch {
        repository.deleteAllPassword()
        repository.insertPasswords(passwordList)
    }

    /**
     * グループデータ一覧の再挿入
     * * 既存のグループ一覧を削除して再度グループ一覧データを挿入する
     *
     * @param groupList 再挿入するグループ一覧
     */
    fun reInsertGroup(groupList: List<GroupEntity>) = viewModelScope.launch {
        repository.deleteAllGroup()
        repository.insertGroups(groupList)
    }
}