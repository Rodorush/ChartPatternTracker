package br.com.rodorush.chartpatterntracker.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import br.com.rodorush.chartpatterntracker.model.PatternDisplayMode
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel.DeleteAccountStatus
import br.com.rodorush.chartpatterntracker.viewmodel.ChartViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit = {}
) {
    val viewModel: ChartViewModel = koinViewModel()
    val currentSource by viewModel.currentSource.collectAsState()
    val displayMode by viewModel.patternDisplayMode.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(currentSource.javaClass.simpleName.removeSuffix("DataSource")) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val authProvider = LocalAuthProvider.current
    val authViewModel: AuthenticationViewModel = viewModel()
    val deleteStatus by authViewModel.deleteAccountStatus.collectAsState()

    var showConfirmDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }

    // Reauth via Google (mesmo padrão de LoginScreen): relança o Sign-In para
    // obter um idToken fresco e completa a exclusão.
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
        authViewModel.reauthenticateAndDeleteWithGoogle(idToken, authProvider)
    }

    LaunchedEffect(deleteStatus) {
        when (deleteStatus) {
            is DeleteAccountStatus.Deleted -> {
                authViewModel.resetDeleteAccountStatus()
                onAccountDeleted()
            }
            is DeleteAccountStatus.RequiresReauth -> showReauthDialog = true
            is DeleteAccountStatus.Error -> {
                val message = (deleteStatus as DeleteAccountStatus.Error).message
                Toast.makeText(
                    context,
                    message ?: context.getString(R.string.network_error),
                    Toast.LENGTH_LONG
                ).show()
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
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 32.dp)
        )

        // Card: Exibição dos padrões (Switch)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.pattern_display_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.pattern_display_only_recent))
                    Switch(
                        checked = displayMode == PatternDisplayMode.ONLY_RECENT,
                        onCheckedChange = { checked ->
                            viewModel.setPatternDisplayMode(
                                if (checked) PatternDisplayMode.ONLY_RECENT else PatternDisplayMode.ALL
                            )
                        },
                        colors = SwitchDefaults.colors()
                    )
                }
            }
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedSource,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.data_source)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("Brapi", "Mock", "AlphaVantage").forEach { source ->
                    DropdownMenuItem(
                        text = { Text(source) },
                        onClick = {
                            selectedSource = source
                            viewModel.setDataSource(source)
                            expanded = false
                        }
                    )
                }
            }
        }

        if (currentSource.requiresApiKey()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.setApiKey(apiKeyInput) },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }

        // Card: Conta (NF-03e — excluir conta)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.account_section_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Divider()
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = deleteStatus != DeleteAccountStatus.Deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delete_account_button))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.back))
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
            text = { Text(stringResource(R.string.delete_account_warning)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    authViewModel.deleteAccount(authProvider)
                }) {
                    Text(
                        text = stringResource(R.string.delete_account_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            }
        )
    }

    if (showReauthDialog) {
        val isGoogle = authProvider.currentUserProviderId() == "google.com"
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                authViewModel.resetDeleteAccountStatus()
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
                        authViewModel.reauthenticateAndDeleteWithPassword(reauthPassword, authProvider)
                        reauthPassword = ""
                    }
                }) {
                    Text(stringResource(R.string.delete_account_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    authViewModel.resetDeleteAccountStatus()
                }) {
                    Text(stringResource(R.string.delete_account_cancel))
                }
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsScreenPreview() {
    AuthProviderWrapper(authProvider = MockAuthProvider(email = "preview@example.com")) {
        SettingsScreen(onNavigateBack = {}, onAccountDeleted = {})
    }
}
