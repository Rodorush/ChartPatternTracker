package br.com.rodorush.chartpatterntracker.ui.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.util.LegalUrls
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.customlogs.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthenticationComponents"

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
fun SocialSignInButton(
    iconId: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .size(60.dp)
            .clickable { onClick() }
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

/**
 * Frase de aceite com "Termos de Uso" e "Política de Privacidade" clicáveis, apontando
 * para [LegalUrls]. O texto vem do template i18n [R.string.terms_policy_agreement]
 * (`%1$s`/`%2$s`) e os trechos dos rótulos viram [LinkAnnotation.Url] — `withLink` abre
 * via `LocalUriHandler`. A varredura por posição evita depender da ordem das palavras
 * entre idiomas. Reaproveitado pelo cadastro e pelo gate de aviso de risco.
 */
@Composable
fun LegalAgreementText(
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    textAlign: TextAlign? = null
) {
    val termsLabel = stringResource(R.string.terms_of_use)
    val privacyLabel = stringResource(R.string.privacy_policy)
    val template = stringResource(R.string.terms_policy_agreement, termsLabel, privacyLabel)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline
        )
    )
    val text = buildAnnotatedString {
        val links = listOf(
            termsLabel to LegalUrls.TERMS,
            privacyLabel to LegalUrls.PRIVACY
        )
        var cursor = 0
        while (cursor <= template.length) {
            val next = links
                .mapNotNull { (label, url) ->
                    val idx = template.indexOf(label, cursor)
                    if (idx >= 0) Triple(idx, label, url) else null
                }
                .minByOrNull { it.first }
            if (next == null) {
                append(template.substring(cursor))
                break
            }
            val (idx, label, url) = next
            if (idx > cursor) append(template.substring(cursor, idx))
            withLink(LinkAnnotation.Url(url, linkStyles)) { append(label) }
            cursor = idx + label.length
        }
    }
    Text(text = text, modifier = modifier, style = style, textAlign = textAlign)
}

/**
 * Texto que a tela mostra para o código de erro do [AuthProvider]. O código sem texto próprio,
 * inclusive `null`, cai no [fallback] da operação.
 *
 * @param fallback o texto genérico da operação (login, cadastro, Perfil).
 * @param invalidCredential o texto de [AuthProvider.INVALID_CREDENTIAL], que depende do que a
 *   tela pediu: e-mail e senha no login, só a senha na reautenticação do Perfil.
 */
@StringRes
internal fun authErrorMessage(
    code: String?,
    @StringRes fallback: Int,
    @StringRes invalidCredential: Int = R.string.invalid_email_password
): Int = when (code) {
    AuthProvider.NETWORK -> R.string.network_error
    AuthProvider.TOO_MANY_REQUESTS -> R.string.auth_error_too_many_requests
    AuthProvider.INVALID_CREDENTIAL -> invalidCredential
    AuthProvider.USER_MISMATCH -> R.string.auth_error_user_mismatch
    AuthProvider.EMAIL_IN_USE -> R.string.email_already_exists
    else -> fallback
}

/**
 * Cria e lembra o fluxo de Google Sign-In (opções + client + launcher) e devolve a ação
 * que dispara o intent. Em sucesso chama [onSuccess]; a falha do seletor ou do Firebase
 * mostra um toast pelo [authErrorMessage], com [R.string.login_error] como texto genérico.
 * O retorno do seletor é triado por [readGoogleSignInResult]: a falha real vai para `Log.e`
 * com a exceção, o que em release vira evento no Crashlytics; o cancelamento fica só em
 * `Log.i`, sem a exceção e sem aviso na tela, e o login já em andamento (toque duplo)
 * é ignorado, porque o seletor aberto antes conclui o fluxo.
 * A navegação fica a cargo do caller (via [onSuccess]), mantendo
 * o componente livre de dependência de tela/Activity. Reaproveitado por Login e Cadastro.
 */
@Composable
fun rememberGoogleSignInLauncher(onSuccess: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val authProvider = LocalAuthProvider.current
    val options = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    val client = remember { GoogleSignIn.getClient(context, options) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val outcome = readGoogleSignInResult(
            GoogleSignIn.getSignedInAccountFromIntent(result.data),
            hasIntent = result.data != null
        )
        when (outcome) {
            is GoogleSignInOutcome.Account ->
                authProvider.signInWithGoogle(outcome.idToken) { success, error ->
                    if (success) {
                        onSuccess()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(authErrorMessage(error, fallback = R.string.login_error)),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            // Fechar o seletor é escolha do usuário, não erro: a tela de login fica como estava.
            is GoogleSignInOutcome.Cancelled ->
                Log.i(
                    TAG,
                    "Login com Google cancelado (resultCode=${result.resultCode}, statusCode=${outcome.statusCode})"
                )
            // O seletor aberto antes conclui o login; este retorno não muda nada na tela.
            GoogleSignInOutcome.InProgress -> Log.i(TAG, "Login com Google já em andamento")
            is GoogleSignInOutcome.Failure -> {
                Log.e(TAG, "Falha no login com Google", outcome.error, "googleSignInStatusCode" to outcome.statusCode)
                Toast.makeText(context, context.getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
    return { launcher.launch(client.signInIntent) }
}

/**
 * Retorno do seletor do Google Sign-In, já triado. Só [Failure] é erro e vai para `Log.e`
 * com a exceção; os demais ficam fora do Crashlytics.
 */
internal sealed interface GoogleSignInOutcome {
    /**
     * Conta escolhida. O [idToken] vem nulo quando o `WEB_CLIENT_ID` não é o client Web, ou
     * quando a task conclui sem conta, o que o `getSignedInAccountFromIntent` não produz.
     */
    data class Account(val idToken: String?) : GoogleSignInOutcome

    /** O usuário fechou o seletor (ver [isGoogleSignInCancelled]). */
    data class Cancelled(val statusCode: Int) : GoogleSignInOutcome

    /**
     * Status 12502: outro seletor já está aberto, em geral por toque duplo no botão. O retorno
     * desse outro seletor conclui o fluxo, então este deve ser ignorado pela tela.
     */
    data object InProgress : GoogleSignInOutcome

    /**
     * Falha real. O [statusCode] fica nulo quando a falha não é `ApiException` e, portanto,
     * não traz status do Google.
     */
    data class Failure(val error: Exception, val statusCode: Int?) : GoogleSignInOutcome
}

/**
 * Lê o retorno do seletor do Google Sign-In e o tria em [GoogleSignInOutcome]. A falha da task
 * que não é `ApiException` chega embrulhada em [RuntimeExecutionException] e vira
 * [GoogleSignInOutcome.Failure], em vez de fechar o app. Usada no login e na reautenticação
 * do Perfil.
 *
 * @param task a task de `GoogleSignIn.getSignedInAccountFromIntent`, que já nasce completa e
 *   nunca cancelada; task pendente ou cancelada não é tratada aqui.
 * @param hasIntent se o seletor devolveu intent (`ActivityResult.data != null`).
 */
internal fun readGoogleSignInResult(
    task: Task<GoogleSignInAccount>,
    hasIntent: Boolean
): GoogleSignInOutcome =
    try {
        GoogleSignInOutcome.Account(task.getResult(ApiException::class.java)?.idToken)
    } catch (e: ApiException) {
        when {
            isGoogleSignInCancelled(hasIntent = hasIntent, statusCode = e.statusCode) ->
                GoogleSignInOutcome.Cancelled(e.statusCode)
            e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
                GoogleSignInOutcome.InProgress
            else -> GoogleSignInOutcome.Failure(e, e.statusCode)
        }
    } catch (e: RuntimeExecutionException) {
        GoogleSignInOutcome.Failure(e, null)
    }

/**
 * Diz se a falha do seletor do Google Sign-In foi cancelamento do usuário, e não erro.
 * Conta como cancelamento o status 12501 e o retorno sem intent, que não traz status do
 * Google porque o seletor fechou sem resposta. A falha de configuração (SHA-1, client ID)
 * volta com intent e não conta.
 *
 * @param hasIntent se o seletor devolveu intent (`ActivityResult.data != null`).
 * @param statusCode o `statusCode` da `ApiException`.
 */
internal fun isGoogleSignInCancelled(hasIntent: Boolean, statusCode: Int): Boolean =
    !hasIntent || statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED

/**
 * Desconecta a conta do client do Google Sign-In e só então chama [launch], que abre o seletor.
 * Com uma conta já conectada no client, o `signInIntent` a devolve sem mostrar o seletor: na
 * reautenticação, a operação seguiria sem o dono escolher a conta. Por isso, se o [signOut]
 * falha, o seletor não é aberto. Cancelar a coroutine durante o [signOut] também impede o
 * [launch], o que protege o launcher da tela que saiu da composição. O client fica desconectado
 * mesmo quando o usuário cancela o seletor; a sessão do Firebase não muda, e nenhum fluxo do app
 * lê a conta guardada no client.
 *
 * @param signOut o `signOut` do mesmo `GoogleSignInClient` cujo `signInIntent` o [launch] abre.
 * @return `null` quando o seletor foi aberto; a falha do [signOut] quando não foi.
 */
internal suspend fun launchGoogleAccountChooser(
    signOut: () -> Task<Void>,
    launch: () -> Unit
): Exception? {
    try {
        signOut().await()
    } catch (e: CancellationException) {
        // Relança o cancelamento da coroutine; a task do signOut cancelada é só uma falha.
        currentCoroutineContext().ensureActive()
        return e
    } catch (e: Exception) {
        return e
    }
    launch()
    return null
}
