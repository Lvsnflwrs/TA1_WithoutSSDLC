package com.example.mobilesurapp.di

import android.content.Context
import com.example.mobilesurapp.UIApp.login.LoginStateViewModel
import com.example.mobilesurapp.api.WebSocketAuthService
import com.example.mobilesurapp.api.WebSocketClient
import com.example.mobilesurapp.repository.LoginRepositoryImpl
import com.example.mobilesurapp.repository.LoginRepository
import com.example.mobilesurapp.usecase.LoginUseCase
import okhttp3.OkHttpClient
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient, gson: Gson): WebSocketClient {
        val websocketClient = WebSocketClient(okHttpClient, gson)
        websocketClient.connect("ws://192.168.100.47:3000")
        return websocketClient
    }

    @Provides
    @Singleton
    fun provideLoginRepository(webSocketClient: WebSocketClient): LoginRepository {
        return LoginRepositoryImpl(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideLoginUseCase(loginRepository: LoginRepository): LoginUseCase {
        return LoginUseCase(loginRepository)
    }

    @Provides
    @Singleton
    fun provideLoginStateViewModel(): LoginStateViewModel {
        return LoginStateViewModel()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }

}