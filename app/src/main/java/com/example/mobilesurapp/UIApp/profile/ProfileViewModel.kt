package com.example.mobilesurapp.UIApp.profile

import android.util.Log
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
class ProfileViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _adminProfile = MutableStateFlow<Admin?>(null)
    val adminProfile: StateFlow<Admin?> = _adminProfile.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var currentAdminId: String? = null

    fun setAdminId(id: String) {
        currentAdminId = id
        fetchProfile()
    }

    private fun fetchProfile() {
        currentAdminId?.let { id ->
            Log.d("ProfileDebug", "Mulai fetchProfile untuk ID: $id")
            _loading.value = true
            _error.value = null
            viewModelScope.launch {
                Log.d("ProfileDebug", "Menunggu balasan dari userProfileRepository...")
                userProfileRepository.getProfile(id).collectLatest { result ->
                    Log.d("ProfileDebug", "Menerima result dari repository: $result")
                    _loading.value = false
                    result.onSuccess { admin ->
                        _adminProfile.value = admin
                        Log.d("adminVal", "isi dari adminProfileValue: ${_adminProfile.value}")
                    }.onFailure { throwable ->
                        _error.value = throwable.message
                        _adminProfile.value = null
                        Log.e("ProfileDebug", "GAGAL mengambil profil: ${throwable.message}")
                    }
                }
            }
        } ?: run {
            Log.e("ProfileDebug", "Admin ID is null. fetchProfile dibatalkan.")
        }
    }
}