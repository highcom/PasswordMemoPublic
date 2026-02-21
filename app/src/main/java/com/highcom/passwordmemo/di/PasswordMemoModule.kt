package com.highcom.passwordmemo.di

import android.content.Context
import com.highcom.passwordmemo.data.DatabaseManager
import com.highcom.passwordmemo.domain.billing.PurchaseManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * パスワードメモアプリDagger Hilt用モジュール定義クラス
 */
@Module
@InstallIn(SingletonComponent::class)
object PasswordMemoModule {

    /**
     * アプリケーションスコープで使う DatabaseManager を提供する。
     *
     * DatabaseManager は内部で Room のインスタンスを管理し、復元時に DB を再生成する機能を持つ。
     * @param context アプリケーションコンテキスト
     * @return シングルトンの [DatabaseManager]
     */
    @Singleton
    @Provides
    fun provideDatabaseManager(@ApplicationContext context: Context): DatabaseManager {
        return DatabaseManager(context)
    }

    /**
     * 購入状態管理マネージャーを提供する。
     *
     * @param context アプリケーションコンテキスト
     * @return シングルトンの [PurchaseManager]
     */
    @Singleton
    @Provides
    fun providePurchaseManager(
        @ApplicationContext context: Context
    ) = PurchaseManager(context)
}
