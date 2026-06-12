package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.AppDatabase
import com.example.data.UserRepository
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    // Initialize Database and Repository
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { UserRepository(database.userDao()) }

    // Initialize AuthViewModel with factory
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "login",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Login Destination
                        composable("login") {
                            LoginScreen(
                                viewModel = authViewModel,
                                onNavigateToSignup = { navController.navigate("signup") },
                                onNavigateToReset = { navController.navigate("reset") },
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }

                        // 2. Signup Destination
                        composable("signup") {
                            SignUpScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.popBackStack() }
                            )
                        }

                        // 3. Password Reset/Recovery Destination
                        composable("reset") {
                            ResetPasswordScreen(
                                viewModel = authViewModel,
                                onNavigateToLogin = { navController.popBackStack() }
                            )
                        }

                        // 4. Secure Dashboard Destination
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = authViewModel,
                                onLogout = {
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
