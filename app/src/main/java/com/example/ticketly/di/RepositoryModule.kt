package com.example.ticketly.di

import com.example.ticketly.repository.AuthRepository
import com.example.ticketly.repository.AuthRepositoryImpl
import com.example.ticketly.repository.CartRepository
import com.example.ticketly.repository.CartRepositoryImpl
import com.example.ticketly.repository.EventsRepository
import com.example.ticketly.repository.EventsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {
    @Binds
    fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Binds
    fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    fun bindCartRepository(impl: CartRepositoryImpl): CartRepository
}
