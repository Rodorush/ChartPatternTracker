package br.com.rodorush.chartpatterntracker.ui.screens.authentication

import android.content.Intent
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import br.com.rodorush.chartpatterntracker.ui.components.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.components.SignInInputField
import br.com.rodorush.chartpatterntracker.ui.components.SocialSignInButton
import br.com.rodorush.chartpatterntracker.activities.MainActivity
import br.com.rodorush.chartpatterntracker.utils.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.utils.MockAuthProvider
import br.com.rodorush.chartpatterntracker.BuildConfig
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit) {
    val context = LocalContext.current
    val authProvider = LocalAuthProvider.current // Obtemos o AuthProvider do CompositionLocal

    // Campos para e-mail e senha
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isButtonEnabled = remember { mutableStateOf(false) }
    LaunchedEffect(email, password) {
        isButtonEnabled.value = email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotBlank()
    }

    // 1. Configurar opções de Sign In com Google (usando ID token do seu projeto Firebase)
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }

    // 2. Criar GoogleSignInClient a partir das opções
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, googleSignInOptions)
    }

    // 3. Lançador para iniciar a Activity do Google Sign-In e capturar o resultado
    val googleSignInLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                // 4. Chamar o método de login com Google do AuthProvider (FirebaseAuthProvider)
                authProvider.signInWithGoogle(idToken) { success, error ->
                    if (success) {
                        Toast.makeText(context, "Login com Google bem-sucedido!", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, MainActivity::class.java))
                    } else {
                        Toast.makeText(
                            context,
                            error ?: "Erro ao logar com Google",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Erro ao obter conta Google: ${e.message}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro desconhecido: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                contentDescription = "Google",
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    googleSignInLauncher.launch(signInIntent)
                }
            )

            SocialSignInButton(
                iconId = R.drawable.f_facebook,
                contentDescription = "Facebook",
                onClick = {
                    Toast.makeText(
                        context,
                        "Login com Facebook ainda não implementado",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            SocialSignInButton(
                iconId = R.drawable.x_logo,
                contentDescription = "Twitter",
                onClick = {
                    Toast.makeText(
                        context,
                        "Login com Facebook ainda não implementado",
                        Toast.LENGTH_SHORT
                    ).show()
                })
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