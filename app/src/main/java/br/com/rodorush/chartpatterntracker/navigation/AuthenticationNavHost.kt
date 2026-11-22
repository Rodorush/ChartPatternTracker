package br.com.rodorush.chartpatterntracker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.ForgotPasswordScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.LoginScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.RegisterScreen
import br.com.rodorush.chartpatterntracker.ui.screen.authentication.VerifyEmailScreen
import br.com.rodorush.chartpatterntracker.util.provider.FirebaseAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider

const val ROUTE_LOGIN = "login"
const val ROUTE_REGISTER = "register"
const val ROUTE_VERIFY_EMAIL = "verify_email"
const val ROUTE_FORGOT_PASSWORD = "forgot_password"

/**
 * Telas de autenticação: login, cadastro, verificação de e-mail e senha esquecida.
 *
 * @param onSignedIn chamado quando o login termina com acesso liberado (Google, e-mail já
 * verificado ou verificação concluída). Quem hospeda decide como abrir o menu e fecha estas telas,
 * para elas não ficarem na pilha embaixo dele.
 */
@Composable
fun AuthenticationNavHost(
    onSignedIn: () -> Unit,
    navController: NavHostController = rememberNavController(),
    authProvider: AuthProvider = FirebaseAuthProvider(),
    startDestination: String = ROUTE_LOGIN
) {
    AuthProviderWrapper(authProvider = authProvider) {
        NavHost(navController = navController, startDestination = startDestination) {
            composable(ROUTE_LOGIN) {
                LoginScreen(
                    onNavigateToRegister = { navController.navigate(ROUTE_REGISTER) },
                    onSignedIn = onSignedIn,
                    onNavigateToVerifyEmail = {
                        navController.navigate(ROUTE_VERIFY_EMAIL) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToForgotPassword = { navController.navigate(ROUTE_FORGOT_PASSWORD) }
                )
            }
            composable(ROUTE_REGISTER) {
                RegisterScreen(
                    onSignedIn = onSignedIn,
                    onNavigateToLogin = { navController.navigate(ROUTE_LOGIN) },
                    onNavigateToVerifyEmail = {
                        navController.navigate(ROUTE_VERIFY_EMAIL) {
                            popUpTo(ROUTE_LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(ROUTE_FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_VERIFY_EMAIL) {
                VerifyEmailScreen(
                    onVerified = onSignedIn,
                    onLogout = {
                        // Uma saída só: o Sair e o aviso de fim de sessão podem chegar os dois
                        // durante a transição, e o segundo, sem a verificação na pilha para o
                        // popUpTo, empilharia outro login.
                        if (navController.currentDestination?.route == ROUTE_VERIFY_EMAIL) {
                            navController.navigate(ROUTE_LOGIN) {
                                popUpTo(ROUTE_VERIFY_EMAIL) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
}
