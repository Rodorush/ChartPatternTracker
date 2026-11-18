package br.com.rodorush.chartpatterntracker.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.rodorush.chartpatterntracker.navigation.AuthenticationNavHost
import br.com.rodorush.chartpatterntracker.navigation.ROUTE_LOGIN
import br.com.rodorush.chartpatterntracker.navigation.ROUTE_VERIFY_EMAIL
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodel.AuthenticationViewModel
import br.com.rodorush.customlogs.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class AuthenticationActivity : ComponentActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.isLoading.value }
        }

        super.onCreate(savedInstanceState)

        // Ícone tocado com a tarefa do app já aberta: fechar devolve a tela que estava em cima,
        // em vez de empilhar outro login ou, com a sessão salva, um segundo menu.
        val isLauncherIntent = intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        if (isRedundantLauncherStart(isTaskRoot = isTaskRoot, isLauncherIntent = isLauncherIntent)) {
            Log.d(TAG, "Ícone com a tarefa já aberta; volta à tela que estava")
            finish()
            return
        }

        FirebaseApp.initializeApp(this)

        val destination = currentStartDestination()
        Log.d(TAG, "Destino inicial: $destination")

        // Sessão salva: vai direto ao menu, antes do setContent, para a tela de login não piscar.
        // O aviso de risco continua garantido pelo gate do AppNavHost, na MainActivity.
        if (destination == StartDestination.MAIN) {
            openMenu()
            return
        }

        val startRoute = if (destination == StartDestination.VERIFY_EMAIL) {
            ROUTE_VERIFY_EMAIL
        } else {
            ROUTE_LOGIN
        }

        setContent {
            ChartPatternTrackerTheme {
                // targetSdk 35 (Android 15) impõe edge-to-edge; safeDrawingPadding mantém o
                // conteúdo das telas de auth fora das barras de sistema e do teclado.
                Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    AuthenticationNavHost(onSignedIn = { openMenu() }, startDestination = startRoute)
                }
            }
        }
    }

    /** Abre o menu e fecha as telas de autenticação, para não ficarem na pilha embaixo dele. */
    private fun openMenu() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    /** Lê a sessão persistida do Firebase Auth, que está disponível também sem rede. */
    private fun currentStartDestination(): StartDestination {
        val user = FirebaseAuth.getInstance().currentUser
        return resolveStartDestination(
            isSignedIn = user != null,
            isPasswordProvider = user?.providerData.orEmpty().any {
                it.providerId == EmailAuthProvider.PROVIDER_ID
            },
            isEmailVerified = user?.isEmailVerified == true
        )
    }

    private companion object {
        const val TAG = "AuthenticationActivity"
    }
}

/** Para onde o app vai ao abrir, conforme a sessão salva. */
internal enum class StartDestination { LOGIN, VERIFY_EMAIL, MAIN }

/**
 * Decide a tela de entrada a partir da sessão persistida do Firebase Auth.
 *
 * Sem usuário, pede login. Conta com provedor de senha e e-mail ainda não verificado vai para a
 * verificação, mesmo que também tenha outro provedor ligado (NF-21). Qualquer outra sessão entra
 * direto no menu, com ou sem rede.
 *
 * @param isPasswordProvider se a conta tem o provedor de e-mail e senha entre os ligados.
 * @param isEmailVerified o valor guardado na sessão local, que só é atualizado com rede.
 */
internal fun resolveStartDestination(
    isSignedIn: Boolean,
    isPasswordProvider: Boolean,
    isEmailVerified: Boolean
): StartDestination = when {
    !isSignedIn -> StartDestination.LOGIN
    isPasswordProvider && !isEmailVerified -> StartDestination.VERIFY_EMAIL
    else -> StartDestination.MAIN
}

/**
 * Diz se esta abertura pelo ícone chegou com a tarefa do app já aberta, caso em que a instância
 * nova deve fechar e deixar à vista a tela que estava em cima.
 *
 * O sistema só traz a tarefa para a frente quando o intent do ícone bate com o intent que criou a
 * tarefa. Se ela nasceu de outro (uma abertura explícita, como `am start -n`), o ícone cria uma
 * AuthenticationActivity nova por cima, que, com a sessão salva, abriria um segundo menu.
 *
 * @param isTaskRoot se esta instância é a primeira da tarefa.
 * @param isLauncherIntent se o intent é o do ícone: ação MAIN com a categoria LAUNCHER.
 */
internal fun isRedundantLauncherStart(isTaskRoot: Boolean, isLauncherIntent: Boolean): Boolean =
    isLauncherIntent && !isTaskRoot
