package com.example.mobilesurapp.UIApp.edit_profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobilesurapp.model.Admin
import com.example.mobilesurapp.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class
EditProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
) : ViewModel() {

    private val _currentAdminProfile = MutableStateFlow<Admin?>(null)
    val currentAdminProfile: StateFlow<Admin?> = _currentAdminProfile.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _updateSuccess = MutableStateFlow<Boolean?>(null)
    val updateSuccess: StateFlow<Boolean?> = _updateSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var adminId: String? = null

    fun initializeProfile(admin: Admin) {
        adminId = admin.id
        _currentAdminProfile.value = admin
        _name.value = admin.name
        _email.value = admin.email
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun updateProfile() {
        val currentId = adminId
        val currentName = _name.value
        val currentEmail = _email.value

        if (currentId == null) {
            _error.value = "Admin ID is missing for profile update."
            _updateSuccess.value = false
            return
        }

        _loading.value = true
        _updateSuccess.value = null
        _error.value = null

        viewModelScope.launch {
            userProfileRepository.updateProfile(currentId, currentName, currentEmail)
                .collectLatest { result ->
                    _loading.value = false
                    result.onSuccess { success ->
                        _updateSuccess.value = success
                        if (success) {
                            _currentAdminProfile.value = _currentAdminProfile.value?.copy(name = currentName, email = currentEmail)
                        }
                    }.onFailure { throwable ->
                        _error.value = throwable.message
                        _updateSuccess.value = false
                    }
                }
        }
    }
}