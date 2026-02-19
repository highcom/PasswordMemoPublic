package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutofillInputViewModel @Inject constructor(private val repository: PasswordMemoRepository) : ViewModel() {
    fun insert(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.insertPassword(passwordEntity) }
}
