package com.example.mobilesurapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.mobilesurapp.navigation.AppNavGraph
import com.example.mobilesurapp.UIApp.login.LoginStateViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.mobilesurapp.background.FaceSyncWorker
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import com.example.mobilesurapp.domain.utils.isDeviceSecure
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.view.WindowManager
import com.example.mobilesurapp.domain.utils.RaspManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val loginStateViewModel: LoginStateViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.decorView.filterTouchesWhenObscured = true
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        RaspManager.secureRuntimeCheck()

        window.decorView.filterTouchesWhenObscured = true

        val syncWorkRequest = PeriodicWorkRequestBuilder<FaceSyncWorker>(
            15, TimeUnit.SECONDS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            FaceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
        

        setContent {
            val context = LocalContext.current
            val isSecure = remember { isDeviceSecure(context) }
            if (!isSecure) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Keamanan Perangkat Lemah") },
                    text = { Text("Aplikasi Mobile Surveillance tidak dapat dijalankan. Harap pasang PIN, Pola, atau Kata Sandi pada perangkat Anda demi keamanan data intelijen.") },
                    confirmButton = {
                        TextButton(onClick = {
                            finish()
                        }) {
                            Text("Tutup Aplikasi")
                        }
                    }
                )
            } else {
                val cameraPermissionState =
                    rememberPermissionState(permission = Manifest.permission.CAMERA)

                LaunchedEffect(Unit) {
                    if (!cameraPermissionState.status.isGranted) {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }

                if (cameraPermissionState.status.isGranted) {
                    val navController = rememberNavController()
                    val initialStartDestination = remember {
                        if (loginStateViewModel.isLoggedIn.value) "camera" else "login"
                    }

                    AppNavGraph(
                        navController = navController,
                        loginStateViewModel = loginStateViewModel,
                        startDestination = initialStartDestination
                    )
                }
            }
        }
    }
}
