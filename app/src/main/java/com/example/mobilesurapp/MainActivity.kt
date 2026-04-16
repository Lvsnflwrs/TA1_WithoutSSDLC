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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val loginStateViewModel: LoginStateViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val syncWorkRequest = PeriodicWorkRequestBuilder<FaceSyncWorker>(
            15, TimeUnit.SECONDS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            FaceSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        setContent {
            val cameraPermissionState =
                rememberPermissionState(permission = Manifest.permission.CAMERA)

            LaunchedEffect(Unit) {
                if (!cameraPermissionState.status.isGranted) {
                    cameraPermissionState.launchPermissionRequest()
                }
            }

            if (cameraPermissionState.status.isGranted) {
                val navController = rememberNavController()
                val isLoggedIn by loginStateViewModel.isLoggedIn.collectAsState()
                AppNavGraph(
                    navController,
                    loginStateViewModel,
                    startDestination = if (isLoggedIn) "camera" else "login")
            }
        }
    }
}
