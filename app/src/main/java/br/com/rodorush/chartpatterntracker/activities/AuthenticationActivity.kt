package br.com.rodorush.chartpatterntracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodels.AuthenticationViewModel

class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }

        super.onCreate(savedInstanceState)

        setContent {
            ChartPatternTrackerTheme {
                AuthenticationNavHost()
            }
        }
    }
}

@Composable
fun AuthenticationNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { LoginScreen(onNavigateToRegister = { navController.navigate("register") }) }
        composable("register") { RegisterScreen(onNavigateToLogin = { navController.navigate("login") }) }
    }
}

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(120.dp)
                .offset(y = 94.dp),
        )
        Text(
            text = stringResource(R.string.sign_in_your_account),
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.offset(y = 269.dp)
        )
        Button(onClick = onNavigateToRegister) {
            Text(text = "Ir para Cadastro")
        }
    }
}

@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Cadastro")
        Button(onClick = onNavigateToLogin) {
            Text(text = "Ir para Login")
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