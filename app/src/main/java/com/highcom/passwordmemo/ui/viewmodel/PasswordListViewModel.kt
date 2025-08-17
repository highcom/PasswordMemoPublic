package com.highcom.passwordmemo.ui.viewmodel

import android.os.Parcelable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.highcom.passwordmemo.data.PasswordEntity
import com.highcom.passwordmemo.data.PasswordMemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * パスワード一覧ビューモデル
 *
 * @property repository データアクセスリポジトリ
 */
@HiltViewModel
class PasswordListViewModel @Inject constructor(private val repository: PasswordMemoRepository) : ViewModel() {
    /** 洗濯中のグループID */
    private val _groupIdFlow = MutableStateFlow(1L)
    /** スクロール位置保存用 */
    var recyclerViewState: Parcelable? = null
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