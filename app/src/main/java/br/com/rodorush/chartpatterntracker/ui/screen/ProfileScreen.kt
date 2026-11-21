package br.com.rodorush.chartpatterntracker.ui.screen

import android.app.Activity
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import br.com.rodorush.chartpatterntracker.ui.component.GoogleSignInOutcome
import br.com.rodorush.chartpatterntracker.ui.component.authErrorMessage
import br.com.rodorush.chartpatterntracker.ui.component.deviceCredentialIntent
import br.com.rodorush.chartpatterntracker.ui.component.launchGoogleAccountChooser
import br.com.rodorush.chartpatterntracker.ui.component.readGoogleSignInResult
import br.com.rodorush.chartpatterntracker.ui.component.requestDeviceCredential
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.DeleteAccountStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.ReauthMethod
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateEmailStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdateNameStatus
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.UpdatePasswordStatus
import br.com.rodorush.chartpatterntracker.viewmodel.resolveReauthMethod
import br.com.rodorush.customlogs.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch

private const val TAG = "ProfileScreen"

/**
 * Operação sensível pendente: à espera da credencial do aparelho (exclusão e e-mail) ou da
 * reautenticação, quando o Firebase exige sessão recente.
 */
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
    // Uma conta pode ter senha e Google vinculados. As opções de senha e e-mail seguem a senha
    // vinculada; a reautenticação segue o provedor com que o usuário entrou.
    val linkedProviders = authProvider.currentUserProviderIds()
    val hasPassword = AuthProvider.PASSWORD_PROVIDER in linkedProviders
    val googleOnly = AuthProvider.GOOGLE_PROVIDER in linkedProviders && !hasPassword
    val sessionProvider by authViewModel.sessionProvider.collectAsState()
    // "Entrada via": o provedor da sessão. Sem ele, só o vinculado único, porque a ordem dos
    // vinculados não diz por qual o usuário entrou.
    val providerLabel = sessionProvider ?: linkedProviders.singleOrNull()

    val nameStatus by authViewModel.updateNameStatus.collectAsState()
    val passwordStatus by authViewModel.updatePasswordStatus.collectAsState()
    val emailStatus by authViewModel.updateEmailStatus.collectAsState()
    val deleteStatus by authViewModel.deleteAccountStatus.collectAsState()
    val resetStatus by authViewModel.resetPasswordStatus.collectAsState()

    var showNameDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf(displayName.orEmpty()) }
    var currentPassword by remember { mutableStateOf("") }
    // Sem rememberSaveable, para a senha não ir ao estado salvo da Activity. A troca de senha não
    // passa pelo seletor do Google (ver openReauthDialog), então nada a retoma depois dele.
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // Salvo para o retorno do seletor do Google retomar a troca de e-mail com o endereço digitado,
    // mesmo com a Activity recriada enquanto o seletor estava aberto. Limpo no envio do link e no
    // cancelamento; depois de um erro, fica no campo para a nova tentativa.
    var newEmail by rememberSaveable { mutableStateOf("") }
    var reauthPassword by remember { mutableStateOf("") }
    // Salvo pelo mesmo motivo, e sem valor padrão: o retorno do seletor ou da tela de credencial do
    // aparelho sem operação restaurada não faz nada, em vez de seguir uma operação que o usuário não
    // pediu, como excluir a conta.
    var pendingReauthOp by rememberSaveable { mutableStateOf<ReauthOp?>(null) }
    // Método do diálogo de reautenticação, fixado ao abri-lo (ver openReauthDialog). Salvo para o
    // aviso de credencial recusada, depois do seletor do Google, sair certo com a Activity recriada.
    var dialogReauthMethod by rememberSaveable { mutableStateOf(ReauthMethod.PASSWORD) }
    // Do Confirmar até o retorno do seletor do Google, passando pelo signOut. Sem esse estado em
    // isBusy, outro botão do Perfil trocaria pendingReauthOp antes do retorno do seletor, e um
    // segundo Confirmar abriria outro seletor. Sem rememberSaveable, para não ficar preso se a
    // Activity for recriada com o seletor aberto.
    var googleChooserActive by remember { mutableStateOf(false) }
    // Do Confirmar ou Salvar até o retorno da tela de credencial do aparelho, pelo mesmo motivo e
    // sem rememberSaveable, como o googleChooserActive.
    var credentialPromptActive by remember { mutableStateOf(false) }

    val isBusy = nameStatus is UpdateNameStatus.Updating ||
        passwordStatus is UpdatePasswordStatus.Updating ||
        emailStatus is UpdateEmailStatus.Updating ||
        deleteStatus == DeleteAccountStatus.Deleting ||
        googleChooserActive ||
        credentialPromptActive

    // Devolve a operação pendente de reautenticação ao repouso, sem aviso na tela.
    fun resetPendingStatus() {
        when (pendingReauthOp) {
            ReauthOp.DELETE -> authViewModel.resetDeleteAccountStatus()
            ReauthOp.PASSWORD -> authViewModel.resetUpdatePasswordStatus()
            ReauthOp.EMAIL -> authViewModel.resetUpdateEmailStatus()
            null -> Unit
        }
    }

    // Os campos de senha são mascarados: o que sobra de uma tentativa somaria, sem o usuário ver,
    // ao que ele digitar na próxima. Por isso limpam em todo fim de troca, com sucesso ou erro.
    fun clearPasswordFields() {
        currentPassword = ""
        newPassword = ""
        confirmPassword = ""
    }

    // O usuário desistiu da operação pendente, na credencial do aparelho ou na reautenticação: volta
    // ao repouso e descarta o que digitou para ela. Um erro não passa por aqui: o e-mail fica no
    // campo para a nova tentativa.
    fun cancelPendingOp() {
        when (pendingReauthOp) {
            ReauthOp.EMAIL -> newEmail = ""
            ReauthOp.PASSWORD -> clearPasswordFields()
            ReauthOp.DELETE, null -> Unit
        }
        resetPendingStatus()
    }

    // Reauth via Google: abre o seletor de contas (ver launchGoogleAccountChooser) para obter
    // um idToken fresco e retoma a operação pendente.
    val scope = rememberCoroutineScope()
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
        // O seletor fechou: daqui em diante, o status da operação retomada mantém o isBusy.
        googleChooserActive = false
        val outcome = readGoogleSignInResult(
            GoogleSignIn.getSignedInAccountFromIntent(result.data),
            hasIntent = result.data != null
        )
        // Na falha, o idToken nulo segue para a ViewModel e encerra a operação com erro; aqui só
        // se registra a causa, que lá não chega. O cancelamento não chega à ViewModel.
        val idToken = when (outcome) {
            is GoogleSignInOutcome.Account -> outcome.idToken.also {
                if (it == null) {
                    // Conta selecionada sem idToken indica configuração errada (WEB_CLIENT_ID).
                    // Com Throwable, para virar evento no Crashlytics.
                    Log.w(TAG, "Reautenticação com Google sem idToken", IllegalStateException("idToken nulo"))
                }
            }
            is GoogleSignInOutcome.Cancelled -> {
                // Fechar o seletor é escolha do usuário, não erro: a operação pendente volta ao
                // repouso sem aviso, como no Cancelar do diálogo de reautenticação.
                Log.i(
                    TAG,
                    "Reautenticação com Google cancelada (resultCode=${result.resultCode}, statusCode=${outcome.statusCode})"
                )
                cancelPendingOp()
                return@rememberLauncherForActivityResult
            }
            GoogleSignInOutcome.InProgress -> {
                // O seletor aberto antes retoma a operação pendente. Encerrar a operação aqui
                // mostraria erro e, com o retorno do outro, a repetiria.
                Log.i(TAG, "Reautenticação com Google já em andamento")
                return@rememberLauncherForActivityResult
            }
            is GoogleSignInOutcome.Failure -> {
                Log.e(
                    TAG,
                    "Falha na reautenticação com Google",
                    outcome.error,
                    "googleSignInStatusCode" to outcome.statusCode
                )
                null
            }
        }
        when (pendingReauthOp) {
            ReauthOp.DELETE -> authViewModel.reauthenticateAndDeleteWithGoogle(idToken, authProvider)
            ReauthOp.EMAIL ->
                authViewModel.reauthAndUpdateEmailWithGoogle(idToken, newEmail, authProvider)
            // A troca de senha não abre o seletor, e sem operação restaurada não há o que retomar.
            ReauthOp.PASSWORD, null -> {
                Log.w(
                    TAG,
                    "Retorno do seletor do Google sem operação de Google pendente",
                    IllegalStateException("pendingReauthOp=$pendingReauthOp")
                )
                resetPendingStatus()
            }
        }
    }

    fun toast(resId: Int) =
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_LONG).show()

    // Segue com a operação pendente depois da credencial do aparelho, ou sem ela no aparelho sem
    // bloqueio de tela. Se o Firebase pedir sessão recente, a reautenticação vem em seguida.
    fun runCredentialConfirmedOp() {
        when (pendingReauthOp) {
            ReauthOp.DELETE -> authViewModel.deleteAccount(authProvider)
            ReauthOp.EMAIL -> authViewModel.updateEmail(newEmail, authProvider)
            // A troca de senha não pede a credencial, e sem operação restaurada não há o que seguir.
            ReauthOp.PASSWORD, null -> {
                Log.w(
                    TAG,
                    "Credencial do aparelho sem operação pendente que a peça",
                    IllegalStateException("pendingReauthOp=$pendingReauthOp")
                )
                resetPendingStatus()
            }
        }
    }

    val credentialLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        credentialPromptActive = false
        if (result.resultCode == Activity.RESULT_OK) {
            runCredentialConfirmedOp()
        } else {
            // Voltar na tela do sistema é escolha do usuário, não erro: a operação volta ao
            // repouso sem aviso, como no Cancelar dos diálogos.
            Log.i(TAG, "Credencial do aparelho cancelada (resultCode=${result.resultCode}, op=$pendingReauthOp)")
            cancelPendingOp()
        }
    }

    // Pede o bloqueio de tela do aparelho antes de excluir a conta ou alterar o e-mail, inclusive
    // na sessão recente, em que o Firebase não pede reautenticação.
    fun confirmDeviceCredentialThen(op: ReauthOp) {
        pendingReauthOp = op
        val description = when (op) {
            ReauthOp.DELETE -> R.string.device_credential_delete_description
            else -> R.string.device_credential_email_description
        }
        val intent = deviceCredentialIntent(
            context,
            title = context.getString(R.string.device_credential_title),
            description = context.getString(description)
        )
        if (intent == null) Log.i(TAG, "Aparelho sem bloqueio de tela; segue sem a credencial (op=$op)")
        credentialPromptActive = intent != null
        val failure = requestDeviceCredential(
            intent,
            launch = { credentialLauncher.launch(it) },
            proceed = { runCredentialConfirmedOp() }
        )
        if (failure != null) {
            credentialPromptActive = false
            Log.e(TAG, "Falha ao abrir a credencial do aparelho", failure, "op" to op.name)
            resetPendingStatus()
            toast(R.string.auth_error_generic)
        }
    }

    // A credencial recusada vem da reautenticação. Pela senha, o texto é "senha incorreta"; pelo
    // Google não há senha digitada, e o texto fica o genérico. A troca de senha sempre reautentica
    // pela senha; as demais operações, pelo método do diálogo.
    fun toastError(code: String?, passwordReauth: Boolean = dialogReauthMethod == ReauthMethod.PASSWORD) =
        toast(
            authErrorMessage(
                code,
                fallback = R.string.auth_error_generic,
                invalidCredential = if (passwordReauth) R.string.auth_error_wrong_password else R.string.auth_error_generic
            )
        )

    // O método fica fixo enquanto o diálogo está aberto. Ele vem do provedor da sessão, que a
    // ViewModel relê antes de pedir a reautenticação. A troca de senha usa sempre a senha, que o
    // usuário acabou de digitar, e não passa pelo seletor do Google.
    fun openReauthDialog(op: ReauthOp) {
        pendingReauthOp = op
        dialogReauthMethod = if (op == ReauthOp.PASSWORD) {
            ReauthMethod.PASSWORD
        } else {
            resolveReauthMethod(authViewModel.sessionProvider.value, linkedProviders)
        }
        showReauthDialog = true
    }

    LaunchedEffect(Unit) {
        authViewModel.loadSessionProvider(authProvider)
    }

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
                clearPasswordFields()
                authViewModel.resetUpdatePasswordStatus()
            }
            is UpdatePasswordStatus.RequiresReauth -> openReauthDialog(ReauthOp.PASSWORD)
            is UpdatePasswordStatus.Error -> {
                toastError(s.message, passwordReauth = true)
                clearPasswordFields()
                authViewModel.resetUpdatePasswordStatus()
            }
            else -> Unit
        }
    }

    // E-mail de redefinição disparado de dentro do diálogo "Alterar senha"
    // (caso o usuário não lembre a senha atual).
    LaunchedEffect(resetStatus) {
        when (resetStatus) {
            AuthenticationViewModel.ResetPasswordStatus.Sent -> {
                toast(R.string.forgot_password_sent_generic)
                authViewModel.resetPasswordResetStatus()
            }
            is AuthenticationViewModel.ResetPasswordStatus.Error -> {
                toast(R.string.forgot_password_send_error)
                authViewModel.resetPasswordResetStatus()
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
            is UpdateEmailStatus.RequiresReauth -> openReauthDialog(ReauthOp.EMAIL)
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
            is DeleteAccountStatus.RequiresReauth -> openReauthDialog(ReauthOp.DELETE)
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
                if (providerLabel != null) {
                    Text(
                        text = stringResource(R.string.profile_provider_label, providerLabel),
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

        if (googleOnly) {
            // Sem senha vinculada, senha e e-mail são geridos pela conta Google (§12.7).
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
        // Exige a senha atual sempre: a troca reautentica antes de atualizar,
        // então só quem sabe a senha vigente consegue alterá-la.
        val canSubmit = currentPassword.isNotBlank() &&
            newPassword.length >= MIN_PASSWORD_LENGTH && newPassword == confirmPassword
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                clearPasswordFields()
            },
            title = { Text(stringResource(R.string.change_password_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(R.string.reauth_password_prompt)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    if (email != null) {
                        TextButton(onClick = {
                            showPasswordDialog = false
                            clearPasswordFields()
                            authViewModel.sendPasswordReset(email, authProvider)
                        }) {
                            Text(stringResource(R.string.forgot_password_link))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canSubmit,
                    onClick = {
                        showPasswordDialog = false
                        authViewModel.reauthAndUpdatePasswordWithPassword(
                            currentPassword, newPassword, authProvider
                        )
                    }
                ) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    clearPasswordFields()
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
                        confirmDeviceCredentialThen(ReauthOp.EMAIL)
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
                    confirmDeviceCredentialThen(ReauthOp.DELETE)
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
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                cancelPendingOp()
            },
            title = { Text(stringResource(R.string.reauth_required)) },
            text = {
                if (dialogReauthMethod == ReauthMethod.GOOGLE) {
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
                    if (googleChooserActive) return@TextButton
                    showReauthDialog = false
                    if (dialogReauthMethod == ReauthMethod.GOOGLE) {
                        googleChooserActive = true
                        scope.launch {
                            // Com o seletor aberto, quem solta a trava é o retorno dele, no
                            // googleReauthLauncher; aqui, só quando o seletor não abriu.
                            var chooserLaunched = false
                            try {
                                val failure = launchGoogleAccountChooser(
                                    signOut = { googleSignInClient.signOut() },
                                    launch = { googleReauthLauncher.launch(googleSignInClient.signInIntent) }
                                )
                                chooserLaunched = failure == null
                                if (failure != null) {
                                    Log.e(TAG, "Falha ao desconectar a conta do Google antes da reautenticação", failure)
                                    resetPendingStatus()
                                    toast(R.string.auth_error_generic)
                                }
                            } finally {
                                if (!chooserLaunched) googleChooserActive = false
                            }
                        }
                    } else {
                        when (pendingReauthOp) {
                            ReauthOp.DELETE ->
                                authViewModel.reauthenticateAndDeleteWithPassword(reauthPassword, authProvider)
                            ReauthOp.PASSWORD ->
                                authViewModel.reauthAndUpdatePasswordWithPassword(reauthPassword, newPassword, authProvider)
                            ReauthOp.EMAIL ->
                                authViewModel.reauthAndUpdateEmailWithPassword(reauthPassword, newEmail, authProvider)
                            null -> Unit
                        }
                        reauthPassword = ""
                    }
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    cancelPendingOp()
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
            displayName = "Preview User"
        )
    ) {
        ProfileScreen(onNavigateBack = {}, onAccountDeleted = {})
    }
}
