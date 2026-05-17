package com.example.mobilesurapp.repository

import android.util.Log
import com.example.mobilesurapp.api.WebSocketClient
import com.example.mobilesurapp.model.Admin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class UserProfileRepositoryImpl @Inject constructor(
    private val webSocketAuthService: WebSocketClient
) : UserProfileRepository {

    override suspend fun getProfile(adminId: String): Flow<Result<Admin>> = callbackFlow {
        webSocketAuthService.onProfileReceived = { admin, errorMessage ->
            if (admin != null) {
                trySend(Result.success(admin))
            } else {
                trySend(Result.failure(Exception(errorMessage ?: "Unknown error fetching profile")))
                Log.e("UserProfileRepository", "Failed to receive profile: $errorMessage")
            }
        }
        webSocketAuthService.requestProfile(adminId)
        awaitClose {
            webSocketAuthService.onProfileReceived = null
        }
    }

    override suspend fun updateProfile(adminId: String, name: String, email: String): Flow<Result<Boolean>> = callbackFlow {
        webSocketAuthService.onProfileUpdateResult = { success, message ->
            if (success) {
                trySend(Result.success(true))
            } else {
                trySend(Result.failure(Exception(message ?: "Unknown error updating profile")))
                Log.e("UserProfileRepository", "Profile update failed: $message")
            }
        }
        webSocketAuthService.updateProfile(adminId, name, email)
        awaitClose {
            webSocketAuthService.onProfileUpdateResult = null
        }
    }
}