package com.example.mobilesurapp.domain.usecase

import javax.inject.Inject
import com.example.mobilesurapp.model.User
import com.example.mobilesurapp.api.ApiResult
import com.example.mobilesurapp.repository.FaceRepository

class RegisterUserWithFaceUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend operator fun invoke(adminId: Int?, name: String, email: String, phone: String, embeddings: FloatArray, role: String): ApiResult<Boolean> {
        val user = User(userId = null, adminId = adminId, name = name, email = email, phone = phone, embeddings = embeddings, role = role)
        return faceRepository.registerUserWithFace(user)
    }
}