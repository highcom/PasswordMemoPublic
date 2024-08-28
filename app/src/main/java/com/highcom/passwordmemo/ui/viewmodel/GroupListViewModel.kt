package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.GroupEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.launch

/**
 * グループ一覧ビューモデル
 *
 * @property repository データアクセスリポジトリ
 */
class GroupListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    /**
     * グループ一覧ビューモデル生成クラス
     *
     * @property repository
     */
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GroupListViewModel(repository) as T
        }
    }

    /** グループ一覧 */
    val groupList = repository.groupList

    /**
     * グループデータ挿入
     *
     * @param groupEntity グループデータ
     */
    fun insert(groupEntity: GroupEntity) = viewModelScope.launch { repository.insertGroup(groupEntity) }

    /**
     * グループデータ一覧の挿入
     *
     * @param groupList グループデータ一覧
     */
    fun insert(groupList: List<GroupEntity>) = viewModelScope.launch { repository.insertGroups(groupList) }

    /**
     * グループデータ更新
     *
     * @param groupEntity グループデータ
     */
    fun update(groupEntity: GroupEntity) = viewModelScope.launch { repository.updateGroup(groupEntity) }

    /**
     * グループデータ一覧の更新
     *
     * @param groupList グループデータ一覧
     */
    fun update(groupList: List<GroupEntity>) = viewModelScope.launch { repository.updateGroups(groupList) }

    /**
     * グループデータ削除
     *
     * @param id 削除対象グループデータID
     */
    fun delete(id: Long) = viewModelScope.launch { repository.deleteGroup(id) }

    /**
     * 指定されたグループIDを処理グループIDにリセットする
     *
     * @param groupId リセットするグループID
     */
    fun resetGroupId(groupId: Long) = viewModelScope.launch { repository.resetGroupId(groupId) }
}