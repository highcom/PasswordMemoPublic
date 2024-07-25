package com.highcom.passwordmemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase

class PasswordMemoApplication : Application() {
    private var _database: PasswordMemoRoomDatabase? = null
    private var _repository: PasswordMemoRepository? = null

    /** パスワードメモデータのアクセスリポジトリ */
    val repository: PasswordMemoRepository
        get() = _repository ?: throw IllegalStateException("Repository not initialized")

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
        initializeDatabaseAndRepository()
    }

    // Method to initialize the database and repository
    /**
     * データベースとリポジトリの初期化処理
     */
    fun initializeDatabaseAndRepository() {
        _database = PasswordMemoRoomDatabase.getDatabase(this)
        _repository = PasswordMemoRepository(_database!!.passwordDao(), _database!!.groupDao())
    }
}