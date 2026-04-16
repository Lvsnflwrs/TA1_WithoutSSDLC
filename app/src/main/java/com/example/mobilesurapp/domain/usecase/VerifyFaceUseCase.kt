package com.example.mobilesurapp.domain.usecase

import com.example.mobilesurapp.model.FaceVerificationResult
import javax.inject.Inject
import com.example.mobilesurapp.api.ApiResult
import com.example.mobilesurapp.repository.FaceRepository

class VerifyFaceUseCase @Inject constructor(
    private val faceRepository: FaceRepository
) {
    suspend operator fun invoke(newFaceEmbeddings: FloatArray): ApiResult<FaceVerificationResult> {
        return faceRepository.verifyFace(newFaceEmbeddings)
    }
}