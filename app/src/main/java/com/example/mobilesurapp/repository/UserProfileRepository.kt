package com.example.mobilesurapp.repository

import com.example.mobilesurapp.model.Admin
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    suspend fun getProfile(adminId: String): Flow<Result<Admin>>
    suspend fun updateProfile(adminId: String, name: String, email: String): Flow<Result<Boolean>>
}