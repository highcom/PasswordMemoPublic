package com.highcom.passwordmemo.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.highcom.passwordmemo.R
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DatabaseManager: 内部で Room インスタンスを保持し、必要に応じて close()/再作成を行えるようにする。
 * UI/Repository はこれを注入して DAO を都度取得するか、dbRecreated をトリガーに Flow を切り替える。
 */
@Singleton
class DatabaseManager @Inject constructor(private val context: Context) {
    companion object {
        private const val DB_NAME = "PasswordMemoDB"
        private const val TMP_DB_NAME = "PasswordMemoDB_tmp"
        private const val TAG = "DatabaseManager"
    }

    private val lock = Any()

    @Volatile
    private var db: PasswordMemoRoomDatabase? = null

    private val _dbRecreated = MutableSharedFlow<Unit>(replay = 1)
    val dbRecreated: SharedFlow<Unit> = _dbRecreated

    /**
     * 現在の Room データベースインスタンスを返却する。
     * インスタンスが未生成であれば生成して保持する。
     *
     * 呼び出し: 任意スレッド。
     * 戻り値: アプリ内で共有される単一の [PasswordMemoRoomDatabase] インスタンス。
     */
    fun getDatabase(): PasswordMemoRoomDatabase {
        var result = db
        if (result == null) {
            synchronized(lock) {
                result = db ?: buildDatabase().also { db = it }
            }
        }
        return result!!
    }

    /**
     * 新しい Room インスタンスを構築する内部ユーティリティ。
     *
     * 内部で SQLCipher の SupportFactory、マイグレーション設定などを適用する。
     * プライベートメソッドのため外部から呼び出す必要はない。
     */
    private fun buildDatabase(): PasswordMemoRoomDatabase {
        // mirror PasswordMemoModule configuration (cipher, migrations, hooks)
        val passphrase = SQLiteDatabase.getBytes(context.getString(R.string.db_secret_key).toCharArray())
        val factory = SupportFactory(passphrase, object : SQLiteDatabaseHook {
            override fun preKey(database: SQLiteDatabase?) {}
            override fun postKey(database: SQLiteDatabase?) {
                val cursor = database?.rawQuery("PRAGMA cipher_migrate", null)
                var migrationOccurred = false
                if (cursor?.count == 1) {
                    cursor.moveToFirst()
                    val selection: String = cursor.getString(0)
                    migrationOccurred = selection == "0"
                    Log.d("selection", selection)
                }
                cursor?.close()
                Log.d("migrationOccurred:", migrationOccurred.toString())
            }
        })

        return Room.databaseBuilder(
            context.applicationContext,
            PasswordMemoRoomDatabase::class.java,
            DB_NAME
        )
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .openHelperFactory(factory)
            .build()
    }

    /**
     * 一時復元ファイル（PasswordMemoDB_tmp）を本体に置換して、
     * Room インスタンスを再生成する。
     *
     * このメソッドは I/O を伴うため suspend であり、呼び出し元は必ず IO スレッド（例: coroutine の Dispatcher.IO）から呼ぶか、
     * lifecycleScope.launch { } などで呼ぶこと。
     *
     * 処理の流れ:
     *  1. 既存の Room インスタンスを閉じる
     *  2. tmp ファイルを本体に移動（rename / copy）
     *  3. 新しい Room インスタンスを build
     *  4. 成功時に [dbRecreated] を emit してリスナーに通知する
     */
    suspend fun replaceDatabaseFromTmp() = withContext(Dispatchers.IO) {
        synchronized(lock) {
            try {
                // Close existing DB
                db?.let {
                    try { it.close() } catch (e: Exception) { Log.w(TAG, "close failed", e) }
                    db = null
                }

                val dest = context.getDatabasePath(DB_NAME)
                val tmp = context.getDatabasePath(TMP_DB_NAME)

                // delete dest and wal/shm if exists
                listOf(dest, File(dest.absolutePath + "-wal"), File(dest.absolutePath + "-shm")).forEach { if (it.exists()) it.delete() }

                // move tmp -> dest (rename)
                if (tmp.exists()) {
                    val moved = tmp.renameTo(dest)
                    if (!moved) {
                        // fallback to copy
                        FileInputStream(tmp).channel.use { input ->
                            FileOutputStream(dest).channel.use { output ->
                                output.transferFrom(input, 0, input.size())
                            }
                        }
                        tmp.delete()
                    }
                } else {
                    Log.w(TAG, "tmp DB not found: ${tmp.path}")
                }

                // delete wal/shm if any leftover
                listOf(File(dest.absolutePath + "-wal"), File(dest.absolutePath + "-shm")).forEach { if (it.exists()) it.delete() }

                // rebuild DB instance
                db = buildDatabase()

            } catch (e: Exception) {
                Log.e(TAG, "replaceDatabaseFromTmp failed", e)
                throw e
            }
        }

        try { _dbRecreated.emit(Unit) } catch (e: Exception) { Log.w(TAG, "emit failed", e) }
    }

    /**
     * 保持している Room インスタンスを閉じ、内部参照を null にする。
     *
     * 呼び出し: 任意スレッド。バックアップ前などに呼ぶことでファイルを安全にコピーできる。
     */
    fun closeDatabase() {
        synchronized(lock) {
            db?.let {
                try { it.close() } catch (e: Exception) { Log.w(TAG, "close failed", e) }
                db = null
            }
        }
    }

    /**
     * データベースファイルの File オブジェクトを返すユーティリティ。
     *
     * @return アプリが使用するデータベースファイルのパス ([PasswordMemoDB])
     */
    fun getDatabasePath(): File = context.getDatabasePath(DB_NAME)
}
