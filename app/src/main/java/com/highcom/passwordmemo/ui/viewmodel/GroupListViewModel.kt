package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupListViewModel(repository) as T
        }
    }

    val groupList = repository.groupList

    fun insert(groupEntity: GroupEntity) = viewModelScope.launch { repository.insertGroup(groupEntity) }
    fun update(groupEntity: GroupEntity) = viewModelScope.launch { repository.updateGroup(groupEntity) }
    fun update(groupList: List<GroupEntity>) = viewModelScope.launch { repository.updateGroups(groupList) }
    fun delete(id: Long) = viewModelScope.launch { repository.deleteGroup(id) }
    fun deleteAll() = viewModelScope.launch { repository.deleteAllGroup() }

    /**
     * 指定されたグループIDを処理グループIDにリセットする
     *
     * @param groupId リセットするグループID
     */
    fun resetGroupId(groupId: Long) = viewModelScope.launch { repository.resetGroupId(groupId) }
}