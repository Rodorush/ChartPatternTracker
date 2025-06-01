package br.com.rodorush.chartpatterntracker.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rodorush.chartpatterntracker.navigation.AuthenticationNavHost
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import com.google.firebase.FirebaseApp

class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            ChartPatternTrackerTheme {
                AuthenticationNavHost()
            }
        }
    }
}