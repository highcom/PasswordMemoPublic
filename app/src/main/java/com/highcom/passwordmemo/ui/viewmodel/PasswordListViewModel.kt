package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class PasswordListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordListViewModel(repository) as T
        }
    }

    private val _groupIdFlow = MutableStateFlow(1L)
    val passwordList: Flow<List<PasswordEntity>> = _groupIdFlow.flatMapLatest { id ->
        repository.getPasswordList(id)
    }

    // TODO:loginDataManagerでグループをセットしている箇所にこのメソッドも呼び出す
    fun setSelectGroup(groupId: Long) {
        _groupIdFlow.value = groupId
    }
    fun insert(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.insertPassword(passwordEntity) }
    fun insert(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.insertPasswords(passwordList) }
    fun update(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.updatePassword(passwordEntity) }
    fun update(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.updatePasswords(passwordList) }
    fun delete(id: Long) = viewModelScope.launch { repository.deletePassword(id) }
}