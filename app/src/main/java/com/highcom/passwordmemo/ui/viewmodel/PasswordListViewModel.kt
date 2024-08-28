package com.highcom.passwordmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * パスワード一覧ビューモデル
 *
 * @property repository データアクセスリポジトリ
 */
class PasswordListViewModel(private val repository: PasswordMemoRepository) : ViewModel() {
    /**
     * パスワード一覧ビューモデル生成クラス
     *
     * @property repository
     */
    class Factory(private val repository: PasswordMemoRepository) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PasswordListViewModel(repository) as T
        }
    }

    /** 洗濯中のグループID */
    private val _groupIdFlow = MutableStateFlow(1L)
    /** パスワード一覧 */
    @ExperimentalCoroutinesApi
    val passwordList: Flow<List<PasswordEntity>> = _groupIdFlow.flatMapLatest { id ->
        repository.getPasswordList(id)
    }

    /**
     * 選択グループ設定処理
     *
     * @param groupId 選択したグループID
     */
    fun setSelectGroup(groupId: Long) {
        _groupIdFlow.value = groupId
    }

    /**
     * パスワードデータ挿入
     *
     * @param passwordEntity パスワードデータ
     */
    fun insert(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.insertPassword(passwordEntity) }

    /**
     * パスワードデータ一覧の挿入
     *
     * @param passwordList パスワードデータ一覧
     */
    fun insert(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.insertPasswords(passwordList) }

    /**
     * パスワードデータ更新
     *
     * @param passwordEntity パスワードデータ
     */
    fun update(passwordEntity: PasswordEntity) = viewModelScope.launch { repository.updatePassword(passwordEntity) }

    /**
     * パスワードデータ一覧の更新
     *
     * @param passwordList パスワードデータ一覧
     */
    fun update(passwordList: List<PasswordEntity>) = viewModelScope.launch { repository.updatePasswords(passwordList) }

    /**
     * パスワードデータ削除
     *
     * @param id 削除対象パスワードデータID
     */
    fun delete(id: Long) = viewModelScope.launch { repository.deletePassword(id) }
}