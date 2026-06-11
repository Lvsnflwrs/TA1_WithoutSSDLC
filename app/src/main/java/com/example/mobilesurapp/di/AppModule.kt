package com.example.mobilesurapp.di

import android.content.Context
import androidx.room.Room
import okhttp3.OkHttpClient
import okhttp3.CertificatePinner
import com.google.gson.Gson
import android.util.Base64
import net.sqlcipher.database.SupportFactory

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
import com.example.mobilesurapp.domain.usecase.GetUserProfileUseCase
import com.example.mobilesurapp.domain.usecase.UpdateUserProfileUseCase
import com.example.mobilesurapp.domain.utils.NetworkUtils
import com.example.mobilesurapp.domain.utils.CryptoManager

import com.example.mobilesurapp.face.FaceEmbedder
import com.example.mobilesurapp.face.FaceNetModel

import com.example.mobilesurapp.modelload.AddFaceDetector

import com.example.mobilesurapp.repository.FaceRepository
import com.example.mobilesurapp.repository.FaceRepositoryImpl
import com.example.mobilesurapp.repository.UserProfileRepository
import com.example.mobilesurapp.repository.UserProfileRepositoryImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import javax.inject.Singleton
import com.example.mobilesurapp.BuildConfig
import java.net.URI

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCryptoManager(): CryptoManager {
        return CryptoManager()
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient, gson: Gson): WebSocketClient {
        val websocketClient = WebSocketClient(okHttpClient, gson)
        websocketClient.connect(BuildConfig.WSS_URL)
        return websocketClient
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val host = URI(BuildConfig.WSS_URL).host
            ?: throw IllegalArgumentException("WSS_URL tidak valid atau kosong")

        val certificatePinner = CertificatePinner.Builder()
            .add(host, "sha256/Cgcdqp4tWwaW/5zVT0SzM3JVarwR9qZzaApjgFqeVic=")
            .add(host, "sha256/SwPJlmyGOywAipWl7ZJUBwRIx7IZ0oMQL2psW26OKs0=")
            .add(host, "sha256/i7WTqTvh0OioIruIfFR4kMPnBqrS2rdiVPl/s2uC/CY=")
            .build()

        return OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .build()
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
    fun provideAppDatabase(@ApplicationContext context: Context, cryptoManager: CryptoManager): AppDatabase {
        val prefs = context.getSharedPreferences("secure_app_prefs", Context.MODE_PRIVATE)
        var encryptedKeyBase64 = prefs.getString("encrypted_db_key", null)

        val dbPassphrase: ByteArray

        if (encryptedKeyBase64 == null) {

            val secureRandom = SecureRandom()
            val newKey = ByteArray(32)
            secureRandom.nextBytes(newKey)

            val encryptedKey = cryptoManager.encrypt(newKey)

            encryptedKeyBase64 = Base64.encodeToString(encryptedKey, Base64.DEFAULT)
            prefs.edit().putString("encrypted_db_key", encryptedKeyBase64).apply()

            dbPassphrase = newKey
        } else {
            val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.DEFAULT)
            dbPassphrase = cryptoManager.decrypt(encryptedKey)
        }
        val factory = SupportFactory(dbPassphrase)
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "faceRecogntionDB"
        ).openHelperFactory(factory).fallbackToDestructiveMigration().build()
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

    @Provides
    @Singleton
    fun provideUserProfileRepository(
        webSocketClient: WebSocketClient
    ): UserProfileRepository {
        return UserProfileRepositoryImpl(webSocketClient)
    }

    @Provides
    @Singleton
    fun provideGetUserProfileUseCase(repository: UserProfileRepository): GetUserProfileUseCase {
        return GetUserProfileUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideUpdateUserProfileUseCase(repository: UserProfileRepository): UpdateUserProfileUseCase {
        return UpdateUserProfileUseCase(repository)
    }

}