package com.highcom.passwordmemo

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.highcom.passwordmemo.data.PasswordMemoRepository
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import com.highcom.passwordmemo.util.login.LoginDataManager

/**
 * パスワードメモアプリ用Applicationクラス
 * * データベースやリポジトリの初期化処理を行う
 *
 */
class PasswordMemoApplication : Application() {
    /** パスワードメモ用データベース */
    private var _database: PasswordMemoRoomDatabase? = null
    /** パスワードメモデータのアクセスリポジトリ */
    private var _repository: PasswordMemoRepository? = null
    /** ログインデータ管理 */
    private var _loginDataManager: LoginDataManager? = null

    /** パスワードメモデータのアクセスリポジトリ */
    val repository: PasswordMemoRepository
        get() = _repository ?: throw IllegalStateException("Repository not initialized")
    /** ログインデータ管理 */
    val loginDataManager: LoginDataManager
        get() = _loginDataManager ?: throw IllegalStateException("LoginDataManager not initialized")

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(PasswordMemoLifecycle())
        initializeDatabaseAndRepository()
        _loginDataManager = LoginDataManager.getInstance(this)
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