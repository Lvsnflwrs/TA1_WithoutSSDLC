package com.example.mobilesurapp.domain.usecase

import com.example.mobilesurapp.model.Admin
import com.example.mobilesurapp.repository.UserProfileRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(profile: Admin): Boolean {
        val adminId = profile.id
        val username = profile.name
        val email = profile.email

        return repository.updateProfile(adminId, username, email).first().getOrThrow()
    }
}