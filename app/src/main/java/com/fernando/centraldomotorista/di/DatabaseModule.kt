package com.fernando.centraldomotorista.di

import android.content.Context
import com.fernando.centraldomotorista.auth.UserSession
import com.fernando.centraldomotorista.data.local.GuardedDatabaseProvider
import com.fernando.centraldomotorista.data.local.LocalDatabaseProvider
import com.fernando.centraldomotorista.data.local.UserDatabaseGuard
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideUserDatabaseGuard(
        @ApplicationContext context: Context,
        userSession: UserSession
    ): UserDatabaseGuard = UserDatabaseGuard(context, userSession)

    @Provides
    @Singleton
    fun provideLocalDatabaseProvider(guard: UserDatabaseGuard): LocalDatabaseProvider =
        GuardedDatabaseProvider(guard)
}
