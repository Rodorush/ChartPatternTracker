package br.com.rodorush.chartpatterntracker.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.activity.MainActivity
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.LoginScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.RegisterScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.VerifyEmailScreen
import br.com.rodorush.chartpatterntracker.util.provider.FirebaseAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider

const val ROUTE_LOGIN = "login"
const val ROUTE_REGISTER = "register"
const val ROUTE_VERIFY_EMAIL = "verify_email"

@Composable
fun AuthenticationNavHost(
    navController: NavHostController = rememberNavController(),
    authProvider: AuthProvider = FirebaseAuthProvider(),
    startDestination: String = ROUTE_LOGIN
) {
    val context = LocalContext.current
    AuthProviderWrapper(authProvider = authProvider) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) },
                    onNavigateToVerifyEmail = {
                        navController.navigate(ROUTE_VERIFY_EMAIL) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_REGISTER) {
                RegisterScreen(
                    onNavigateToLogin = { navController.navigate(ROUTE_LOGIN) },
                    onNavigateToVerifyEmail = {
                        navController.navigate(ROUTE_VERIFY_EMAIL) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_VERIFY_EMAIL) {
                VerifyEmailScreen(
                    onVerified = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    },
                    onLogout = {
                        navController.navigate(ROUTE_LOGIN) {
                            popUpTo(ROUTE_VERIFY_EMAIL) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
