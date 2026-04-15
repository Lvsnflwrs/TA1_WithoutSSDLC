package com.example.mobilesurapp.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import com.example.mobilesurapp.UIApp.Camera.CameraScreen
import com.example.mobilesurapp.UIApp.Camera.CameraViewModel
import com.example.mobilesurapp.UIApp.login.LoginScreen
import com.example.mobilesurapp.UIApp.login.LoginStateViewModel
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable

@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginStateViewModel: LoginStateViewModel,
    startDestination: String
) {
    val activityViewModelStoreOwner =
        checkNotNull(LocalContext.current as? androidx.lifecycle.ViewModelStoreOwner) {
            "AppNavGraph harus berada dalam konteks ViewModelStoreOwner"
        }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    loginStateViewModel.login()
                    navController.navigate("camera") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("camera") {
            val cameraViewModel =
                hiltViewModel<CameraViewModel>(viewModelStoreOwner = activityViewModelStoreOwner)
            CameraScreen(
                viewModel = cameraViewModel,
            )
        }
    }
}