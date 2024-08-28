package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.launch

/**
 * ログイン画面のビューモデル
 *
 * @property repository データアクセスリポジトリ
 */
class LoginViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    /**
     * ログイン画面ビューモデル生成クラス
     *
     * @property repository
     */
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(repository) as T
        }
    }

    /**
     * 全てのデータのデータ削除
     *
     * @param groupEntity リセット後の初期グループデータ
     */
    fun reset(groupEntity: GroupEntity) = viewModelScope.launch {
        repository.deleteAllPassword()
        repository.deleteAllGroup()
        repository.insertGroup(groupEntity)
    }
}