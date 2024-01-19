package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.launch

class PasswordListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordListViewModel(repository) as T
        }
    }
    fun insert(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.insertPassword(passwordEntity) }
    fun update(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.updatePassword(passwordEntity) }
    fun update(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.updatePasswords(passwordList) }
    fun delete(id: Long) = viewModelScope.launch { repository.deletePassword(id) }
    fun deleteAll() = viewModelScope.launch { repository.deleteAllPassword() }
}