package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.LoginScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.RegisterScreen
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.FirebaseAuthProvider

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