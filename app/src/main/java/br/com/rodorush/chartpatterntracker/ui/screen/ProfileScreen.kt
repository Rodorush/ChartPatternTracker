package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.DeleteAccountStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateEmailStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateNameStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdatePasswordStatus
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/** Operação destrutiva pendente quando o Firebase exige sessão recente. */
private enum class ReauthOp { DELETE, PASSWORD, EMAIL }

private const val MIN_PASSWORD_LENGTH = 6

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit = {},
    authViewModel: AuthenticationViewModel = viewModel()
) {
    val context = LocalContext.current
    val authProvider = LocalAuthProvider.current

    val email = authProvider.currentUserEmail()
    val displayName = authProvider.currentUserDisplayName()
    val providerId = authProvider.currentUserProviderId()
    val isGoogle = providerId == "google.com"

    val nameStatus by authViewModel.updateNameStatus.collectAsState()
    val passwordStatus by authViewModel.updatePasswordStatus.collectAsState()
    val emailStatus by authViewModel.updateEmailStatus.collectAsState()
    val deleteStatus by authViewModel.deleteAccountStatus.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf(displayName.orEmpty()) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newEmail by remember { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    var pendingReauthOp by remember { mutableStateOf(ReauthOp.DELETE) }

    val isBusy = nameStatus is UpdateNameStatus.Updating ||
        passwordStatus is UpdatePasswordStatus.Updating ||
        emailStatus is UpdateEmailStatus.Updating ||
        deleteStatus == DeleteAccountStatus.Deleting

    // Reauth via Google (mesmo padrão de LoginScreen/SettingsScreen): relança o
    // Sign-In para obter um idToken fresco e retoma a operação pendente.
    val googleSignInClient = remember {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, options)
    }
    val googleReauthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        val idToken = try {
            task.getResult(ApiException::class.java)?.idToken
        } catch (e: ApiException) {
            null
        }
        when (pendingReauthOp) {
            ReauthOp.DELETE -> authViewModel.reauthenticateAndDeleteWithGoogle(idToken, authProvider)
            ReauthOp.PASSWORD ->
                authViewModel.reauthAndUpdatePasswordWithGoogle(idToken, newPassword, authProvider)
            ReauthOp.EMAIL ->
                authViewModel.reauthAndUpdateEmailWithGoogle(idToken, newEmail, authProvider)
        }
    }

    fun toast(resId: Int) =
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show()

    fun toastError(message: String?) =
        Toast.makeText(
            context,
            message ?: context.getString(R.string.network_error),
            Toast.LENGTH_LONG
        ).show()

    LaunchedEffect(nameStatus) {
        when (val s = nameStatus) {
            is UpdateNameStatus.Updated -> {
                toast(R.string.name_updated)
                authViewModel.resetUpdateNameStatus()
            }
            is UpdateNameStatus.Error -> {
                toastError(s.message)
                authViewModel.resetUpdateNameStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(passwordStatus) {
        when (val s = passwordStatus) {
            is UpdatePasswordStatus.Updated -> {
                toast(R.string.password_updated)
                newPassword = ""
                confirmPassword = ""
                authViewModel.resetUpdatePasswordStatus()
            }
            is UpdatePasswordStatus.RequiresReauth -> {
                pendingReauthOp = ReauthOp.PASSWORD
                showReauthDialog = true
            }
            is UpdatePasswordStatus.Error -> {
                toastError(s.message)
                authViewModel.resetUpdatePasswordStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(emailStatus) {
        when (val s = emailStatus) {
            is UpdateEmailStatus.VerificationSent -> {
                toast(R.string.email_change_pending)
                newEmail = ""
                authViewModel.resetUpdateEmailStatus()
            }
            is UpdateEmailStatus.RequiresReauth -> {
                pendingReauthOp = ReauthOp.EMAIL
                showReauthDialog = true
            }
            is UpdateEmailStatus.Error -> {
                toastError(s.message)
                authViewModel.resetUpdateEmailStatus()
            }
            else -> Unit
        }
    }

    LaunchedEffect(deleteStatus) {
        when (val s = deleteStatus) {
            is DeleteAccountStatus.Deleted -> {
                authViewModel.resetDeleteAccountStatus()
                onAccountDeleted()
            }
            is DeleteAccountStatus.RequiresReauth -> {
                pendingReauthOp = ReauthOp.DELETE
                showReauthDialog = true
            }
            is DeleteAccountStatus.Error -> {
                toastError(s.message)
                authViewModel.resetDeleteAccountStatus()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.profile),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (email != null) {
                    Text(text = email, style = MaterialTheme.typography.titleMedium)
                }
                if (!displayName.isNullOrBlank()) {
                    Text(text = displayName, style = MaterialTheme.typography.bodyLarge)
                }
                if (providerId != null) {
                    Text(
                        text = stringResource(R.string.profile_provider_label, providerId),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Alterar nome vale para todos: updateProfile funciona e persiste mesmo
        // em contas Google (altera só o displayName do Firebase).
        Button(
            onClick = { nameInput = displayName.orEmpty(); showNameDialog = true },
            enabled = !isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.change_name_title))
        }

        if (isGoogle) {
            // Senha e e-mail são geridos pela conta Google (§12.7).
            Text(
                text = stringResource(R.string.profile_google_managed_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Button(
                onClick = { showPasswordDialog = true },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.change_password_title))
            }
            Button(
                onClick = { showEmailDialog = true },
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.change_email_title))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { showDeleteDialog = true },
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.delete_account_button))
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.back))
        }
    }

    // --- Diálogo: alterar nome ---
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(stringResource(R.string.change_name_title)) },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.display_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = nameInput.isNotBlank(),
                    onClick = {
                        showNameDialog = false
                        authViewModel.updateDisplayName(nameInput.trim(), authProvider)
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- Diálogo: alterar senha ---
    if (showPasswordDialog) {
        val tooShort = newPassword.isNotEmpty() && newPassword.length < MIN_PASSWORD_LENGTH
        val mismatch = confirmPassword.isNotEmpty() && newPassword != confirmPassword
        val canSubmit = newPassword.length >= MIN_PASSWORD_LENGTH && newPassword == confirmPassword
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                newPassword = ""
                confirmPassword = ""
            },
            title = { Text(stringResource(R.string.change_password_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.new_password)) },
                        singleLine = true,
                        isError = tooShort,
                        supportingText = if (tooShort) {
                            { Text(stringResource(R.string.password_too_short)) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text(stringResource(R.string.confirm_new_password)) },
                        singleLine = true,
                        isError = mismatch,
                        supportingText = if (mismatch) {
                            { Text(stringResource(R.string.passwords_do_not_match)) }
                        } else null,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        showPasswordDialog = false
                        authViewModel.updatePassword(newPassword, authProvider)
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    newPassword = ""
                    confirmPassword = ""
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    // --- Diálogo: alterar e-mail ---
    if (showEmailDialog) {
        val emailValid = newEmail.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(newEmail.trim()).matches()
        AlertDialog(
            onDismissRequest = { showEmailDialog = false; newEmail = "" },
            title = { Text(stringResource(R.string.change_email_title)) },
            text = {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text(stringResource(R.string.new_email)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = emailValid,
                    onClick = {
                        showEmailDialog = false
                        newEmail = newEmail.trim()
                        authViewModel.updateEmail(newEmail, authProvider)
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false; newEmail = "" }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // --- Diálogo: confirmar exclusão ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    authViewModel.deleteAccount(authProvider)
                }) {
                    Text(
                        text = stringResource(R.string.delete_account_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            }
        )
    }

    // --- Diálogo de reauth (compartilhado entre delete/senha/e-mail) ---
    if (showReauthDialog) {
        fun resetPendingStatus() {
            when (pendingReauthOp) {
                ReauthOp.DELETE -> authViewModel.resetDeleteAccountStatus()
                ReauthOp.PASSWORD -> authViewModel.resetUpdatePasswordStatus()
                ReauthOp.EMAIL -> authViewModel.resetUpdateEmailStatus()
            }
        }
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                resetPendingStatus()
            },
            title = { Text(stringResource(R.string.reauth_required)) },
            text = {
                if (isGoogle) {
                    Text(stringResource(R.string.reauth_with_google))
                } else {
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text(stringResource(R.string.reauth_password_prompt)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showReauthDialog = false
                    if (isGoogle) {
                        googleReauthLauncher.launch(googleSignInClient.signInIntent)
                    } else {
                        when (pendingReauthOp) {
                            ReauthOp.DELETE ->
                                authViewModel.reauthenticateAndDeleteWithPassword(reauthPassword, authProvider)
                            ReauthOp.PASSWORD ->
                                authViewModel.reauthAndUpdatePasswordWithPassword(reauthPassword, newPassword, authProvider)
                            ReauthOp.EMAIL ->
                                authViewModel.reauthAndUpdateEmailWithPassword(reauthPassword, newEmail, authProvider)
                        }
                        reauthPassword = ""
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    resetPendingStatus()
                }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun ProfileScreenPreview() {
    AuthProviderWrapper(
        authProvider = MockAuthProvider(
            email = "preview@example.com",
            displayName = "Preview User",
            providerId = "password"
        )
    ) {
        ProfileScreen(onNavigateBack = {}, onAccountDeleted = {})
    }
}
