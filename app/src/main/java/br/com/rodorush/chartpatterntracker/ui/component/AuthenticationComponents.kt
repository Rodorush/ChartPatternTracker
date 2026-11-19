package br.com.rodorush.chartpatterntracker.ui.component

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException

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
 * ([R.string.login_error]) e registra a [ApiException] com o `statusCode` em `Log.e`, o que
 * em release vira evento no Crashlytics. O cancelamento (fechar o seletor de contas, ou o
 * retorno sem intent) fica só em `Log.i`, sem a exceção, para não poluir o Crashlytics.
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
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            authProvider.signInWithGoogle(account?.idToken) { success, error ->
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
        } catch (e: ApiException) {
            if (isGoogleSignInCancelled(result, e)) {
                Log.i(
                    TAG,
                    "Login com Google cancelado (resultCode=${result.resultCode}, statusCode=${e.statusCode})"
                )
            } else {
                Log.e(TAG, "Falha no login com Google", e, "googleSignInStatusCode" to e.statusCode)
            }
            Toast.makeText(context, context.getString(R.string.login_error), Toast.LENGTH_SHORT).show()
        }
    }
    return { launcher.launch(client.signInIntent) }
}

/**
 * Diz se a falha do seletor do Google Sign-In foi cancelamento do usuário, e não erro.
 * Conta como cancelamento o status 12501 e o retorno sem intent, que não traz status do
 * Google porque o seletor fechou sem resposta. A falha de configuração (SHA-1, client ID)
 * volta com intent e não conta. Usada no login e na reautenticação do Perfil, para que o
 * cancelamento fique fora do Crashlytics nos dois.
 */
internal fun isGoogleSignInCancelled(result: ActivityResult, e: ApiException): Boolean =
    result.data == null || e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED
