package br.com.rodorush.chartpatterntracker.ui.screen.authentication

import android.widget.Toast
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.component.authErrorMessage
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.VerificationStatus

@Composable
fun VerifyEmailScreen(
    onVerified: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthenticationViewModel = viewModel()
) {
    val authProvider = LocalAuthProvider.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cooldown by viewModel.resendCooldownSeconds.collectAsState()
    val status by viewModel.verificationStatus.collectAsState()
    val isChecking by viewModel.isCheckingVerification.collectAsState()
    val isSending by viewModel.isSendingVerification.collectAsState()

    // Lido uma vez: com a sessão encerrada, o Firebase pode limpar o usuário antes de a tela sair, e
    // o texto ficaria sem o endereço durante a saída.
    val email = remember(authProvider) { authProvider.currentUserEmail() ?: "" }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkVerified(authProvider)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(status) {
        when (val s = status) {
            VerificationStatus.Verified -> onVerified()
            VerificationStatus.Sent -> {
                Toast.makeText(context, context.getString(R.string.verify_email_sent), Toast.LENGTH_SHORT).show()
                viewModel.resetVerificationStatus()
            }
            VerificationStatus.NotYetVerified -> {
                Toast.makeText(context, context.getString(R.string.verify_email_not_yet), Toast.LENGTH_LONG).show()
                viewModel.resetVerificationStatus()
            }
            is VerificationStatus.SendError -> {
                val message = authErrorMessage(s.code, fallback = R.string.verify_email_send_error)
                Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
                viewModel.resetVerificationStatus()
            }
            is VerificationStatus.CheckError -> {
                val message = authErrorMessage(s.code, fallback = R.string.verify_email_check_error)
                Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
                viewModel.resetVerificationStatus()
            }
            VerificationStatus.SessionLost -> {
                // Mesmo caminho do Sair: o signOut não tem efeito se o Firebase já encerrou a sessão.
                Toast.makeText(context, context.getString(R.string.verify_email_session_ended), Toast.LENGTH_LONG).show()
                viewModel.resetVerificationStatus()
                authProvider.logout { onLogout() }
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
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.logo),
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.verify_email_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.email_sent_check_inbox, email),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.checkVerified(authProvider) },
            enabled = !isChecking,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(text = stringResource(R.string.verify_email_check_again))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.sendVerificationEmail(authProvider) },
            enabled = cooldown == 0 && !isSending,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            val label = if (cooldown > 0) {
                stringResource(R.string.resend_cooldown, cooldown)
            } else {
                stringResource(R.string.resend_email)
            }
            Text(text = label)
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = {
            authProvider.logout {
                onLogout()
            }
        }) {
            Text(text = stringResource(R.string.logout))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VerifyEmailScreenPreview() {
    AuthProviderWrapper(authProvider = MockAuthProvider(email = "preview@example.com")) {
        VerifyEmailScreen()
    }
}
