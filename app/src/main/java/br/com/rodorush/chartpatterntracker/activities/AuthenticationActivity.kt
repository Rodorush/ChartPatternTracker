package br.com.rodorush.chartpatterntracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rodorush.chartpatterntracker.navigation.AuthenticationNavHost
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodels.AuthenticationViewModel

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

@Preview(showBackground = true)
@Composable
fun AuthenticationNavHostPreview() {
    ChartPatternTrackerTheme {
        AuthenticationNavHost()
    }
}