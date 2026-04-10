package com.example.mobilesurapp.repository

interface LoginRepository {
    suspend fun loginUser(email: String, password: String): Result<String>
}