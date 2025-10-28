package com.highcom.passwordmemo.data

import android.annotation.SuppressLint
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * SQLiteからRoomへのマイグレーション操作
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            // 新しいテーブルを一時テーブルとして構築(passworddata)
            db.execSQL("""
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
            db.execSQL("""
                INSERT OR IGNORE INTO passworddata_tmp (id,title,account,password,url,memo,inputdate)
                SELECT id,title,account,password,url,memo,inputdate FROM passworddata
                """.trimIndent()
            )
            // 旧テーブルを削除
            db.execSQL("DROP TABLE passworddata")
            // 新テーブルをリネーム
            db.execSQL("ALTER TABLE passworddata_tmp RENAME TO passworddata")

            // 新しいテーブルを構築(groupdata)
            db.execSQL("""
                CREATE TABLE groupdata(
                    group_id INTEGER PRIMARY KEY NOT NULL,
                    group_order INTEGER NOT NULL,
                    name TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            // レコードを作成
            db.execSQL("""
                INSERT INTO groupdata VALUES(1, 1, 'すべて')
                """.trimIndent()
            )

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * SQLCipherのv3系からv4系へのマイグレーション
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    @SuppressLint("Range")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            // 新しいテーブルを一時テーブルとして構築(passworddata)
            db.execSQL("""
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
            val cursor = db.query("SELECT id FROM passworddata ORDER BY id;")

            while (cursor.moveToNext()) {
                val currentId = cursor.getInt(cursor.getColumnIndex("id"))
                db.execSQL(
                    "UPDATE passworddata SET id = ? WHERE id = ?",
                    arrayOf<Any>(newId, currentId)
                )
                newId++
            }
            cursor.close()

            // 旧テーブルのデータを全て一時テーブルに追加
            db.execSQL("""
                INSERT OR IGNORE INTO passworddata_tmp (id,title,account,password,url,group_id,memo,inputdate)
                SELECT id,title,account,password,url,group_id,memo,inputdate FROM passworddata
                """.trimIndent()
            )
            // 旧テーブルを削除
            db.execSQL("DROP TABLE passworddata")
            // 新テーブルをリネーム
            db.execSQL("ALTER TABLE passworddata_tmp RENAME TO passworddata")

            // 新しいテーブルを一時テーブルとして構築(groupdata)
            db.execSQL("""
                CREATE TABLE groupdata_tmp(
                    group_id INTEGER PRIMARY KEY NOT NULL,
                    group_order INTEGER NOT NULL,
                    name TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )
            // 旧テーブルのデータを全て一時テーブルに追加
            db.execSQL("""
                INSERT INTO groupdata_tmp (group_id,group_order,name)
                SELECT group_id,group_order,name FROM groupdata
                """.trimIndent()
            )
            // 旧テーブルを削除
            db.execSQL("DROP TABLE groupdata")
            // 新テーブルをリネーム
            db.execSQL("ALTER TABLE groupdata_tmp RENAME TO groupdata")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * 色設定カラムの追加によるマイグレーション
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    @SuppressLint("Range")
    override fun migrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()
        try {
            // 新しいテーブルを一時テーブルとして構築(passworddata)
            db.execSQL("""
                CREATE TABLE passworddata_tmp(
                    id INTEGER PRIMARY KEY NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    account TEXT NOT NULL DEFAULT '',
                    password TEXT NOT NULL DEFAULT '',
                    url TEXT NOT NULL DEFAULT '',
                    group_id INTEGER NOT NULL DEFAULT 1,
                    memo TEXT NOT NULL DEFAULT '',
                    inputdate TEXT NOT NULL DEFAULT '',
                    color INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )

            // 旧テーブルのデータを全て一時テーブルに追加
            db.execSQL("""
                INSERT OR IGNORE INTO passworddata_tmp (id,title,account,password,url,group_id,memo,inputdate)
                SELECT id,title,account,password,url,group_id,memo,inputdate FROM passworddata
                """.trimIndent()
            )
            // 旧テーブルを削除
            db.execSQL("DROP TABLE passworddata")
            // 新テーブルをリネーム
            db.execSQL("ALTER TABLE passworddata_tmp RENAME TO passworddata")

            // 新しいテーブルを一時テーブルとして構築(groupdata)
            db.execSQL("""
                CREATE TABLE groupdata_tmp(
                    group_id INTEGER PRIMARY KEY NOT NULL,
                    group_order INTEGER NOT NULL,
                    name TEXT NOT NULL DEFAULT '',
                    color INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            // 旧テーブルのデータを全て一時テーブルに追加
            db.execSQL("""
                INSERT INTO groupdata_tmp (group_id,group_order,name)
                SELECT group_id,group_order,name FROM groupdata
                """.trimIndent()
            )
            // 旧テーブルを削除
            db.execSQL("DROP TABLE groupdata")
            // 新テーブルをリネーム
            db.execSQL("ALTER TABLE groupdata_tmp RENAME TO groupdata")

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}

/**
 * パスワードメモRoomデータベース生成
 *
 */
@Database(entities = [PasswordEntity::class, GroupEntity::class], version = 5, exportSchema = false)
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
}