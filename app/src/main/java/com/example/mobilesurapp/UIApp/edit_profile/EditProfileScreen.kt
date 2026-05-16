package com.example.mobilesurapp.UIApp.edit_profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.mobilesurapp.UIApp.components.TopBar
import com.example.mobilesurapp.model.Admin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    adminId: String,
    initialName: String,
    initialEmail: String,
    onBackClick: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val updateSuccess by viewModel.updateSuccess.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        val currentAdmin = Admin(
            id = adminId,
            name = initialName,
            email = initialEmail,
            role = ""
        )
        viewModel.initializeProfile(currentAdmin)
    }

    Scaffold(
        topBar = {
            TopBar(
                title = "Edit Profil",
                onBackClick = onBackClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Nama Lengkap") },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = { viewModel.updateProfile() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simpan Perubahan")
                }
            }

            updateSuccess?.let { success ->
                if (success) {
                    Text("Profil berhasil diperbarui!", color = MaterialTheme.colorScheme.primary)
                } else {
                    Text("Gagal memperbarui profil.", color = MaterialTheme.colorScheme.error)
                }
            }

            error?.let { errorMessage ->
                Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}