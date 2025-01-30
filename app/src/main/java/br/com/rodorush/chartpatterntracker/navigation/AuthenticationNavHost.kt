package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.ui.components.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.screens.authentication.LoginScreen
import br.com.rodorush.chartpatterntracker.ui.screens.authentication.RegisterScreen
import br.com.rodorush.chartpatterntracker.utils.AuthProvider
import br.com.rodorush.chartpatterntracker.utils.FirebaseAuthProvider

@Composable
fun AuthenticationNavHost(
    navController: NavHostController = rememberNavController(),
    authProvider: AuthProvider = FirebaseAuthProvider() // Padrão para a implementação real
) {
    AuthProviderWrapper(authProvider = authProvider) {
        NavHost(navController = navController, startDestination = "login") {
            composable("login") { LoginScreen(onNavigateToRegister = { navController.navigate("register") }) }
            composable("register") { RegisterScreen(onNavigateToLogin = { navController.navigate("login") }) }
        }
    }
}