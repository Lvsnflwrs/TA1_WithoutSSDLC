package com.example.mobilesurapp.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import com.example.mobilesurapp.UIApp.Camera.CameraScreen
import com.example.mobilesurapp.UIApp.Camera.CameraViewModel
import com.example.mobilesurapp.UIApp.login.LoginScreen
import com.example.mobilesurapp.UIApp.login.LoginStateViewModel
import com.example.mobilesurapp.UIApp.addFace.AddFaceScreen
import com.example.mobilesurapp.UIApp.login.BiometricLoginScreen
import com.example.mobilesurapp.UIApp.login.ReAuthScreen
import com.example.mobilesurapp.UIApp.profile.ProfileScreen
import com.example.mobilesurapp.UIApp.edit_profile.EditProfileScreen


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
                onLoginSuccess = { adminId ->

                    loginStateViewModel.setLoggedInAdmin(adminId)

                    navController.navigate("camera") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToBiometricLogin = {
                    navController.navigate("BiometricLogin")
                }
            )
        }
        composable("camera") {
            val cameraViewModel =
                hiltViewModel<CameraViewModel>(viewModelStoreOwner = activityViewModelStoreOwner)
            CameraScreen(
                viewModel = cameraViewModel,
                onNavigateToProfile = {navController.navigate("profile")}
            )
        }

        composable("addFace") {
            AddFaceScreen(
                navController = navController,
                loginStateViewModel = loginStateViewModel,
                onNavigateToCamera = {navController.navigate("login")}
            )
        }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                loginStateViewModel = loginStateViewModel,
                onNavigateToEditProfile = { adminId, name, email ->
                    val target = "UpdateProfile/$adminId/$name/$email"
                    val safeTarget = URLEncoder.encode(target, StandardCharsets.UTF_8.toString())
                    navController.navigate("reauth/$safeTarget")
                }
            )
        }

        composable(
            route = "reauth/{targetDestination}",
            arguments = listOf(
                navArgument("targetDestination") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val targetDestination = backStackEntry.arguments?.getString("targetDestination") ?: "camera"

            ReAuthScreen(
                onLoginSuccess = { adminId ->
                    loginStateViewModel.setLoggedInAdmin(adminId)

                    navController.navigate(targetDestination) {
                        popUpTo("reauth/$targetDestination") { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("BiometricLogin"){
            BiometricLoginScreen(
                onLoginSuccess = { adminId ->

                    loginStateViewModel.setLoggedInAdmin(adminId)

                    navController.navigate("camera") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToAddFace = { navController.navigate("reauth/addFace") },

                onNavigateBack = { navController.popBackStack()}
            )
        }

        composable(
            route = "UpdateProfile/{adminId}/{name}/{email}",
            arguments = listOf(
                navArgument("adminId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val adminId = backStackEntry.arguments?.getString("adminId") ?: ""
            val name = backStackEntry.arguments?.getString("name") ?: ""
            val email = backStackEntry.arguments?.getString("email") ?: ""

            EditProfileScreen(
                adminId = adminId,
                initialName = name,
                initialEmail = email,
                onBackClick = { navController.popBackStack("profile", inclusive = false) }
            )
        }
    }
}