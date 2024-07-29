package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.launch

class SettingViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingViewModel(repository) as T
        }
    }

    val passwordList = repository.getPasswordList(1L)
    val groupList = repository.groupList
    fun reInsertPassword(passwordList: List<PasswordEntity>) = viewModelScope.launch {
        repository.deleteAllPassword()
        repository.insertPasswords(passwordList)
    }

    fun reInsertGroup(groupList: List<GroupEntity>) = viewModelScope.launch {
        repository.deleteAllGroup()
        repository.insertGroups(groupList)
    }
}