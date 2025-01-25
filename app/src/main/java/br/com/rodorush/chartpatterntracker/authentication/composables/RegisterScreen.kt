package br.com.rodorush.chartpatterntracker.authentication.composables

import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.authentication.utils.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.authentication.utils.MockAuthProvider
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit = {}
) {
    val authProvider = LocalAuthProvider.current // Obtemos o AuthProvider do CompositionLocal

    // Estados para os campos
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isButtonEnabled = remember {
        mutableStateOf(false)
    }

    LaunchedEffect(name, email, password, confirmPassword, acceptTerms) {
        isButtonEnabled.value = name.isNotBlank() &&
                email.isNotBlank() &&
                Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.isNotBlank() &&
                confirmPassword.isNotBlank() &&
                password == confirmPassword &&
                acceptTerms
    }

    // Layout principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Ícones de navegação e logo no topo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToLogin) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.baseline_arrow_back_24), // Substitua pelo seu ícone de voltar
                    contentDescription = stringResource(R.string.back)
                )
            }
            // Logo à direita
            Icon(
                painter = painterResource(id = R.drawable.logo), // Substitua pelo seu recurso de logo
                contentDescription = stringResource(R.string.logo),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(48.dp)
            )
        }

        // Coluna central com conteúdo
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.create_your_account),
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Campo Nome
            SignInInputField(
                label = stringResource(R.string.name),
                placeholder = stringResource(R.string.name),
                value = name,
                onValueChange = { name = it }
            )

            Spacer(modifier = Modifier.padding(8.dp))

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

            Spacer(modifier = Modifier.padding(8.dp))

            // Campo Password
            SignInInputField(
                label = stringResource(R.string.confirme_a_senha),
                placeholder = "********",
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                isPassword = true
            )

            // Checkbox + Texto: Terms & policy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = acceptTerms,
                    onCheckedChange = { acceptTerms = it }
                )
                Text(
                    text = stringResource(R.string.i_understood_the_terms_policy),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Botão SIGN UP
            Button(
                onClick = {
                    authProvider.createUserWithEmailAndPassword(email, password) { success, error ->
                        if (success) {
                            Toast.makeText(context, "Cadastro realizado com sucesso!", Toast.LENGTH_SHORT).show()
                            onNavigateToLogin()
                        } else {
                            val errorMessage = when (error) {
                                "The email address is already in use by another account." ->
                                    context.getString(R.string.email_already_exists)

                                "A network error (such as timeout, interrupted connection or unreachable host) has occurred." ->
                                    context.getString(R.string.network_error)
                                else ->
                                    context.getString(R.string.registration_error)
                            }
                            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = isButtonEnabled.value
            ) {
                Text(text = stringResource(R.string.sign_up))
            }

            // Texto "or sign up with"
            Text(
                text = stringResource(R.string.or_sign_up_with),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .alpha(0.7f)
            )

            // Seção de ícones (Google, Facebook, Twitter)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Substituir pelos ícones adequados
                Icon(
                    painter = painterResource(id = R.drawable.google_g_logo),
                    contentDescription = "Google",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Google */ },
                    tint = Color.Unspecified // Caso queira manter as cores originais
                )
                Icon(
                    painter = painterResource(id = R.drawable.f_facebook),
                    contentDescription = "Facebook",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Facebook */ },
                    tint = Color.Unspecified
                )
                Icon(
                    painter = painterResource(id = R.drawable.x_logo),
                    contentDescription = "Twitter",
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { /* Ação de login Twitter */ },
                    tint = Color.Unspecified
                )
            }
        }

        // Texto no rodapé para já ter conta
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = stringResource(R.string.have_an_account))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.sign_in),
                style = TextStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                ),
                modifier = Modifier.clickable { onNavigateToLogin() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    AuthProviderWrapper(authProvider = MockAuthProvider()) {
        RegisterScreen(onNavigateToLogin = {})
    }
}