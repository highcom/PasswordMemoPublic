package com.highcom.passwordmemo.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * パスワードメモアプリDagger Hilt用モジュール定義クラス
 */
@Module
@InstallIn(SingletonComponent::class)
object PasswordMemoModule {
    // constructor injection (@Inject) is used for DatabaseManager and PurchaseManager,
    // so explicit @Provides methods are not needed here unless complex initialization is required.
}
