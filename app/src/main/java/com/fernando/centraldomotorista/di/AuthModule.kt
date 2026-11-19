package com.fernando.centraldomotorista.di

import com.fernando.centraldomotorista.auth.SupabaseUserSession
import com.fernando.centraldomotorista.auth.UserSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindUserSession(impl: SupabaseUserSession): UserSession
}
