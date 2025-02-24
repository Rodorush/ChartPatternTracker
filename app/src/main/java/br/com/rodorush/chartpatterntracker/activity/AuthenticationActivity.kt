package br.com.rodorush.chartpatterntracker.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rodorush.chartpatterntracker.navigation.AuthenticationNavHost
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel

class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)

        setContent {
            ChartPatternTrackerTheme {
                AuthenticationNavHost()
            }
        }
    }
}