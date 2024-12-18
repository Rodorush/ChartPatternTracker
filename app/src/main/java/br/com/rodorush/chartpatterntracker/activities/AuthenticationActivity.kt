package br.com.rodorush.chartpatterntracker.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.padding(16.dp))

        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(120.dp)
        )

        Spacer(modifier = Modifier.padding(24.dp))

        // Título
        Text(
            text = stringResource(R.string.sign_in_your_account),
            style = MaterialTheme.typography.displayMedium,
        )

        Spacer(modifier = Modifier.padding(24.dp))

        // Campo Email
        SignInInputField(
            label = "Email",
            placeholder = "email@domain.com",
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Campo Password
        SignInInputField(
            label = "Password",
            placeholder = "********",
            value = password,
            onValueChange = { password = it },
            isPassword = true
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = { /* Lógica de login */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "SIGN IN")
        }

        Spacer(modifier = Modifier.padding(16.dp))

        Text(
            text = "or sign in with",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.padding(16.dp))

        // Botões Sociais
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SocialSignInButton(iconId = R.drawable.logo, contentDescription = "Google")
            SocialSignInButton(iconId = R.drawable.logo, contentDescription = "Facebook")
            SocialSignInButton(iconId = R.drawable.logo, contentDescription = "Twitter")
        }

        Spacer(modifier = Modifier.padding(16.dp))

        // Texto "Don't have an account?"
        Row {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "SIGN UP",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@Composable
fun SignInInputField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    isPassword: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun SocialSignInButton(iconId: Int, contentDescription: String) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(60.dp)
            .clickable { /* Ação do botão social */ }
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp)
            )
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
        Text(text = "Register")
        Button(onClick = onNavigateToLogin) {
            Text(text = "Go to Login")
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