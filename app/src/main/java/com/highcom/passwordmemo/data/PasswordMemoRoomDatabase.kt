package com.highcom.passwordmemo.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.highcom.passwordmemo.R
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory

/**
 * SQLiteからRoomへのマイグレーション操作
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            // 新しいテーブルを一時テーブルとして構築(passworddata)
            database.execSQL("""
                CREATE TABLE passworddata_tmp(
                    id INTEGER PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    account TEXT NOT NULL DEFAULT '',
                    password TEXT NOT NULL DEFAULT '',
                    url TEXT NOT NULL DEFAULT '',
                    group_id INTEGER NOT NULL DEFAULT 1,
                    memo TEXT NOT NULL DEFAULT '',
                    inputdate TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            // 旧テーブルのデータを全て一時テーブルに追加
            database.execSQL("""
                INSERT OR IGNORE INTO passworddata_tmp (id,title,account,password,url,memo,inputdate)
                SELECT id,title,account,password,url,memo,inputdate FROM passworddata
                """.trimIndent()
            )
            // 旧テーブルを削除
            database.execSQL("DROP TABLE passworddata")
            // 新テーブルをリネーム
            database.execSQL("ALTER TABLE passworddata_tmp RENAME TO passworddata")

            // 新しいテーブルを構築(groupdata)
            database.execSQL("""
                CREATE TABLE groupdata(
                    group_id INTEGER PRIMARY KEY NOT NULL,
                    group_order INTEGER NOT NULL,
                    name TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            // レコードを作成
            database.execSQL("""
                INSERT INTO groupdata VALUES(1, 1, 'すべて')
                """.trimIndent()
            )

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    @SuppressLint("Range")
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            // 新しいテーブルを一時テーブルとして構築(passworddata)
            database.execSQL("""
                CREATE TABLE passworddata_tmp(
                    id INTEGER PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    account TEXT NOT NULL DEFAULT '',
                    password TEXT NOT NULL DEFAULT '',
                    url TEXT NOT NULL DEFAULT '',
                    group_id INTEGER NOT NULL DEFAULT 1,
                    memo TEXT NOT NULL DEFAULT '',
                    inputdate TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )

            // 以前のバージョンではidがプライマリキーになってないので重複していた可能性がある
            // なので改めて1から順番にIDを再設定する
            var newId = 1
            val cursor = database.query("SELECT id FROM passworddata ORDER BY id;")

            while (cursor.moveToNext()) {
                val currentId = cursor.getInt(cursor.getColumnIndex("id"))
                database.execSQL(
                    "UPDATE passworddata SET id = ? WHERE id = ?",
                    arrayOf<Any>(newId, currentId)
                )
                newId++
            }
            cursor.close()

            // 旧テーブルのデータを全て一時テーブルに追加
            database.execSQL("""
                INSERT OR IGNORE INTO passworddata_tmp (id,title,account,password,url,group_id,memo,inputdate)
                SELECT id,title,account,password,url,group_id,memo,inputdate FROM passworddata
                """.trimIndent()
            )
            // 旧テーブルを削除
            database.execSQL("DROP TABLE passworddata")
            // 新テーブルをリネーム
            database.execSQL("ALTER TABLE passworddata_tmp RENAME TO passworddata")

            // 新しいテーブルを一時テーブルとして構築(groupdata)
            database.execSQL("""
                CREATE TABLE groupdata_tmp(
                    group_id INTEGER PRIMARY KEY NOT NULL,
                    group_order INTEGER NOT NULL,
                    name TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            // 旧テーブルのデータを全て一時テーブルに追加
            database.execSQL("""
                INSERT INTO groupdata_tmp (group_id,group_order,name)
                SELECT group_id,group_order,name FROM groupdata
                """.trimIndent()
            )
            // 旧テーブルを削除
            database.execSQL("DROP TABLE groupdata")
            // 新テーブルをリネーム
            database.execSQL("ALTER TABLE groupdata_tmp RENAME TO groupdata")

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}

/**
 * パスワードメモRoomデータベース生成
 *
 */
@Database(entities = [PasswordEntity::class, GroupEntity::class], version = 4, exportSchema = false)
abstract class PasswordMemoRoomDatabase : RoomDatabase() {
    /**
     * パスワードデータアクセスオブジェクト
     *
     * @return パスワードデータアクセスオブジェクト
     */
    abstract fun passwordDao(): PasswordDao

    /**
     * グループデータアクセスオブジェクト
     *
     * @return グループデータアクセスオブジェクト
     */
    abstract fun groupDao(): GroupDao

    companion object {
        @Volatile
        private var INSTANCE: PasswordMemoRoomDatabase? = null

        /**
         * データベース取得処理
         *
         * @param context コンテキスト
         * @return データベースインスタンス
         */
        fun getDatabase(
            context: Context,
        ): PasswordMemoRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                // 4.4.2を3.5.9@aarに下げるとSupportFactoryが使えなくなるためSQLCipher4にアップグレード
                // net.sqlcipher.database.SQLiteException: file is not a database: , while compiling: select count(*) from sqlite_master;
                // 上記エラーが発生するのでSQLiteDatabaseHookにてPRAGMAの設定をする必要がある
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordMemoRoomDatabase::class.java,
                    "PasswordMemoDB"
                ).allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(context.getString(R.string.db_secret_key).toCharArray()), object : SQLiteDatabaseHook {
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

                    }))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * データベースのクローズ処理
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }

}