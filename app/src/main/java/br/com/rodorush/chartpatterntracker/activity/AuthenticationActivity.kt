package br.com.rodorush.chartpatterntracker.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rodorush.chartpatterntracker.navigation.AuthenticationNavHost
import br.com.rodorush.chartpatterntracker.navigation.ROUTE_LOGIN
import br.com.rodorush.chartpatterntracker.navigation.ROUTE_VERIFY_EMAIL
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        val startDestination = resolveStartDestination()

        setContent {
            ChartPatternTrackerTheme {
                // targetSdk 35 (Android 15) impõe edge-to-edge; safeDrawingPadding mantém o
                // conteúdo das telas de auth fora das barras de sistema e do teclado.
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    AuthenticationNavHost(startDestination = startDestination)
                }
            }
        }
    }

    private fun resolveStartDestination(): String {
        val user = FirebaseAuth.getInstance().currentUser ?: return ROUTE_LOGIN
        val isPasswordProvider = user.providerData.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
        }
        return if (isPasswordProvider && !user.isEmailVerified) {
            ROUTE_VERIFY_EMAIL
        } else {
            ROUTE_LOGIN
        }
    }
}
