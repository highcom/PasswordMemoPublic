package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PasswordListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordListViewModel(repository) as T
        }
    }

    private var _passwordList = MutableStateFlow<List<PasswordEntity>>(emptyList())
    val passwordList = _passwordList.asStateFlow()

    // TODO:loginDataManagerでグループをセットしている箇所にこのメソッドも呼び出す
    fun setSelectGroup(groupId: Long) {
        _passwordList = repository.getPasswordList(groupId) as MutableStateFlow<List<PasswordEntity>>
    }
    fun insert(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.insertPassword(passwordEntity) }
    fun update(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.updatePassword(passwordEntity) }
    fun update(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.updatePasswords(passwordList) }
    fun delete(id: Long) = viewModelScope.launch { repository.deletePassword(id) }
    fun deleteAll() = viewModelScope.launch { repository.deleteAllPassword() }

    /**
     * 全データの初期化処理
     * パスワードデータとグループデータを削除して初期グループを登録し直す
     *
     */
    fun resetAll() = viewModelScope.launch {
        repository.deleteAllPassword()
        repository.deleteAllGroup()
        repository.insertGroup(GroupEntity(1, 1, ""))
    }
}