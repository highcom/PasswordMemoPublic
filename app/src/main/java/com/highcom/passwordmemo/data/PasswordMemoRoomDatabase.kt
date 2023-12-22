package com.highcom.passwordmemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.highcom.passwordmemo.R
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordMemoRoomDatabase::class.java,
                    "PasswordMemoDB"
                ).openHelperFactory(SupportFactory(SQLiteDatabase.getBytes(context.getString(R.string.db_secret_key).toCharArray())))
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}