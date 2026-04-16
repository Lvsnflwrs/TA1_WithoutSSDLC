package com.example.mobilesurapp.di

import android.content.Context
import androidx.room.Room
import com.example.mobilesurapp.UIApp.login.LoginStateViewModel
import com.example.mobilesurapp.api.WebSocketClient
import com.example.mobilesurapp.database.AppDatabase
import com.example.mobilesurapp.database.dao.PendingSyncDao
import com.example.mobilesurapp.database.dao.UserDao
import com.example.mobilesurapp.repository.LoginRepositoryImpl
import com.example.mobilesurapp.repository.LoginRepository
import com.example.mobilesurapp.domain.usecase.LoginUseCase
import com.example.mobilesurapp.domain.usecase.RegisterUserWithFaceUseCase
import com.example.mobilesurapp.domain.usecase.SyncOfflineFacesUseCase
import com.example.mobilesurapp.domain.usecase.VerifyFaceUseCase
import com.example.mobilesurapp.domain.utils.NetworkUtils
import com.example.mobilesurapp.face.FaceEmbedder
import com.example.mobilesurapp.face.FaceNetModel
import com.example.mobilesurapp.modelload.AddFaceDetector
import com.example.mobilesurapp.repository.FaceRepository
import com.example.mobilesurapp.repository.FaceRepositoryImpl
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

    @Provides
    @Singleton
    fun provideUserDao(appDatabase: AppDatabase): UserDao {
        return appDatabase.userDao()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "faceRecogntionDB"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }

    @Provides
    @Singleton
    fun providePendingSyncDao(appDatabase: AppDatabase): PendingSyncDao {
        return appDatabase.pendingSyncDao()
    }

    @Provides
    @Singleton
    fun provideFaceRepository(
        userDao: UserDao,
        pendingSyncDao: PendingSyncDao,
        webSocketClient: WebSocketClient,
        networkUtils: NetworkUtils
    ): FaceRepository {
        return FaceRepositoryImpl(userDao, pendingSyncDao, webSocketClient, networkUtils)
    }

    @Provides
    @Singleton
    fun provideRegisterFaceUseCase(faceRepository: FaceRepository): RegisterUserWithFaceUseCase {
        return RegisterUserWithFaceUseCase(faceRepository)
    }

    @Provides
    @Singleton
    fun provideVerifyFaceUseCase(faceRepository: FaceRepository): VerifyFaceUseCase {
        return VerifyFaceUseCase(faceRepository)
    }

    @Provides
    @Singleton
    fun provideFaceNetModel(@ApplicationContext context: Context): FaceNetModel {
        return FaceNetModel(context)
    }

    @Provides
    @Singleton
    fun provideFaceEmbedder(faceNetModel: FaceNetModel): FaceEmbedder {
        return FaceEmbedder(faceNetModel)
    }

    @Provides
    @Singleton
    fun provideAddFaceDetector(@ApplicationContext context: Context): AddFaceDetector {
        return AddFaceDetector(context)
    }

    @Provides
    @Singleton
    fun provideSyncOfflineFacesUseCase(
        faceRepository: FaceRepository,
        webSocketClient: WebSocketClient,
        networkUtils: NetworkUtils
    ): SyncOfflineFacesUseCase {
        return SyncOfflineFacesUseCase(faceRepository, webSocketClient, networkUtils)
    }



}