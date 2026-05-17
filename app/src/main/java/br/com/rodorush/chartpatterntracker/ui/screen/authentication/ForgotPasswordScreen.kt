package br.com.rodorush.chartpatterntracker.ui.screen.authentication

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.component.SignInInputField
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.ResetPasswordStatus

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val authProvider = LocalAuthProvider.current
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    val cooldown by viewModel.resendCooldownSeconds.collectAsState()
    val status by viewModel.resetPasswordStatus.collectAsState()

    val isEmailValid = remember(email) {
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    val isSending = status is ResetPasswordStatus.Sending
    val canSubmit = isEmailValid && cooldown == 0 && !isSending

    LaunchedEffect(status) {
        when (status) {
            ResetPasswordStatus.Sent -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.forgot_password_sent_generic),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetPasswordResetStatus()
            }
            is ResetPasswordStatus.Error -> {
                Toast.makeText(
                    context,
                    context.getString(R.string.forgot_password_send_error),
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetPasswordResetStatus()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.logo),
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.forgot_password_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.forgot_password_instructions),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        SignInInputField(
            label = stringResource(R.string.forgot_password_email_label),
            placeholder = stringResource(R.string.email_example),
            value = email,
            onValueChange = { email = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.sendPasswordReset(email.trim(), authProvider) },
            enabled = canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                val label = if (cooldown > 0) {
                    stringResource(R.string.resend_cooldown, cooldown)
                } else {
                    stringResource(R.string.forgot_password_send)
                }
                Text(text = label)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateBack) {
            Text(text = stringResource(R.string.forgot_password_back_to_login))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordScreenPreview() {
    AuthProviderWrapper(authProvider = MockAuthProvider()) {
        ForgotPasswordScreen()
    }
}
