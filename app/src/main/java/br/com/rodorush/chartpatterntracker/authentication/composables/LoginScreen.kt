package br.com.rodorush.chartpatterntracker.authentication.composables

import android.content.Intent
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.activities.MainActivity
import br.com.rodorush.chartpatterntracker.authentication.utils.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.authentication.utils.MockAuthProvider

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit) {
    val authProvider = LocalAuthProvider.current // Obtemos o AuthProvider do CompositionLocal
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isButtonEnabled = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(email, password) {
        isButtonEnabled.value = email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotBlank()
    }

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
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.padding(24.dp))

        // Campo Email
        SignInInputField(
            label = "Email",
            placeholder = stringResource(R.string.email_example),
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Campo Password
        SignInInputField(
            label = stringResource(R.string.password),
            placeholder = "********",
            value = password,
            onValueChange = { password = it },
            isPassword = true
        )

        Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = {
                authProvider.signInWithEmailAndPassword(email, password) { success, error ->
                    if (success) {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    } else {
                        val errorMessage = when {
                            error?.contains("The supplied auth credential is incorrect") == true -> context.getString(
                                R.string.invalid_email_password
                            )

                            else -> {
                                context.getString(R.string.login_error)
                            }
                        }
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = isButtonEnabled.value
        ) {
            Text(text = stringResource(R.string.sign_in))
        }

        Spacer(modifier = Modifier.padding(8.dp))

        Text(
            text = stringResource(R.string.or_sign_in_with),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.padding(8.dp))

        // Botões Sociais
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth(),
        ) {
            SocialSignInButton(
                iconId = R.drawable.google_g_logo,
                contentDescription = "Google"
            )
            SocialSignInButton(
                iconId = R.drawable.f_facebook,
                contentDescription = "Facebook"
            )
            SocialSignInButton(iconId = R.drawable.x_logo, contentDescription = "Twitter")
        }

        Spacer(modifier = Modifier.padding(16.dp))

        // Texto "Don't have an account?"
        Row {
            Text(
                text = stringResource(R.string.don_t_have_an_account),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.sign_up),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    AuthProviderWrapper(authProvider = MockAuthProvider()) {
        LoginScreen(onNavigateToRegister = {})
    }
}