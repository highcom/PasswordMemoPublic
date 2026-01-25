package com.highcom.passwordmemo.di

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.highcom.passwordmemo.R
import com.highcom.passwordmemo.data.MIGRATION_2_3
import com.highcom.passwordmemo.data.MIGRATION_3_4
import com.highcom.passwordmemo.data.MIGRATION_4_5
import com.highcom.passwordmemo.data.PasswordMemoRoomDatabase
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteDatabaseHook
import net.sqlcipher.database.SupportFactory

/**
 * パスワードメモアプリDagger Hilt用モジュール定義クラス
 */
@Module
@InstallIn(ActivityRetainedComponent::class)
object PasswordMemoModule {

    /**
     * パスワードメモRoomデータベースプロバイダ
     *
     * @param context コンテキスト
     */
    @ActivityRetainedScoped
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ) = Room.databaseBuilder(
        context.applicationContext,
        PasswordMemoRoomDatabase::class.java,
        "PasswordMemoDB"
    ).allowMainThreadQueries()
        .fallbackToDestructiveMigration()
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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


    /**
     * パスワードデータアクセスオブジェクトプロバイダ
     *
     * @param db データベース
     */
    @ActivityRetainedScoped
    @Provides
    fun providePasswordDao(db: PasswordMemoRoomDatabase) = db.passwordDao()

    /**
     * グループデータアクセスオブジェクトプロバイダ
     *
     * @param db データベース
     */
    @ActivityRetainedScoped
    @Provides
    fun provideGroupDao(db: PasswordMemoRoomDatabase) = db.groupDao()

    /**
     * 購入状態管理マネージャープロバイダ
     *
     * @param context アプリケーションコンテキスト
     */
    @ActivityRetainedScoped
    @Provides
    fun providePurchaseManager(
        @ApplicationContext context: Context
    ) = PurchaseManager(context)
}
