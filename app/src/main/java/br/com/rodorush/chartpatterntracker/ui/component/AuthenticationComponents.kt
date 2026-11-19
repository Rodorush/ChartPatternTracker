package br.com.rodorush.chartpatterntracker.ui.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import br.com.rodorush.customlogs.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.android.gms.tasks.Task

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
 * Cria e lembra o fluxo de Google Sign-In (opções + client + launcher) e devolve a ação
 * que dispara o intent. Em sucesso chama [onSuccess]; em falha mostra um toast genérico
 * ([R.string.login_error]). O retorno do seletor é triado por [readGoogleSignInResult]: a
 * falha real vai para `Log.e` com a exceção, o que em release vira evento no Crashlytics;
 * o cancelamento e o login já em andamento ficam só em `Log.i`, sem a exceção.
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
                            error ?: context.getString(R.string.login_error),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            is GoogleSignInOutcome.NotAnError -> {
                Log.i(
                    TAG,
                    "Login com Google interrompido sem erro (resultCode=${result.resultCode}, statusCode=${outcome.statusCode})"
                )
                Toast.makeText(context, context.getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
            is GoogleSignInOutcome.Failure -> {
                Log.e(TAG, "Falha no login com Google", outcome.error, "googleSignInStatusCode" to outcome.statusCode)
                Toast.makeText(context, context.getString(R.string.login_error), Toast.LENGTH_SHORT).show()
            }
        }
    }
    return { launcher.launch(client.signInIntent) }
}

/** Retorno do seletor do Google Sign-In, já triado entre conta, interrupção sem erro e falha. */
internal sealed interface GoogleSignInOutcome {
    /** Conta escolhida. O [idToken] vem nulo quando o `WEB_CLIENT_ID` não é o client Web. */
    data class Account(val idToken: String?) : GoogleSignInOutcome

    /** Cancelamento ou login já em andamento: vai para `Log.i`, fora do Crashlytics. */
    data class NotAnError(val statusCode: Int) : GoogleSignInOutcome

    /**
     * Falha real, que vai para `Log.e` com [error]. O [statusCode] fica nulo quando a falha
     * não é `ApiException` e, portanto, não traz status do Google.
     */
    data class Failure(val error: Exception, val statusCode: Int?) : GoogleSignInOutcome
}

/**
 * Lê o retorno do seletor do Google Sign-In sem deixar exceção escapar. A falha que não é
 * `ApiException` chega embrulhada em [RuntimeExecutionException] e vira [GoogleSignInOutcome.Failure],
 * em vez de fechar o app. Usada no login e na reautenticação do Perfil.
 *
 * @param task a task de `GoogleSignIn.getSignedInAccountFromIntent`, que já nasce completa.
 * @param hasIntent se o seletor devolveu intent (`ActivityResult.data != null`).
 */
internal fun readGoogleSignInResult(
    task: Task<GoogleSignInAccount>,
    hasIntent: Boolean
): GoogleSignInOutcome =
    try {
        GoogleSignInOutcome.Account(task.getResult(ApiException::class.java)?.idToken)
    } catch (e: ApiException) {
        if (isExpectedGoogleSignInFailure(hasIntent = hasIntent, statusCode = e.statusCode)) {
            GoogleSignInOutcome.NotAnError(e.statusCode)
        } else {
            GoogleSignInOutcome.Failure(e, e.statusCode)
        }
    } catch (e: RuntimeExecutionException) {
        GoogleSignInOutcome.Failure(e, null)
    }

/**
 * Diz se a falha do seletor do Google Sign-In é interrupção esperada, e não erro. Conta o
 * cancelamento (status 12501, ou retorno sem intent, que não traz status do Google porque o
 * seletor fechou sem resposta) e o login já em andamento (12502, do toque duplo no botão).
 * A falha de configuração (SHA-1, client ID) volta com intent e não conta.
 *
 * @param hasIntent se o seletor devolveu intent (`ActivityResult.data != null`).
 * @param statusCode o `statusCode` da `ApiException`.
 */
internal fun isExpectedGoogleSignInFailure(hasIntent: Boolean, statusCode: Int): Boolean =
    !hasIntent ||
        statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED ||
        statusCode == GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS
