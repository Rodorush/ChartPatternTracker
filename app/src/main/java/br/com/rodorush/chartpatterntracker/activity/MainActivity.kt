package br.com.rodorush.chartpatterntracker.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.compose.rememberNavController
import br.com.rodorush.chartpatterntracker.navigation.AppNavHost
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.util.AppPreferences
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider
import br.com.rodorush.chartpatterntracker.util.provider.FirebaseAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.FirebasePatternProvider
import br.com.rodorush.customlogs.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val appPreferences: AppPreferences by inject()

    private val exitGate = SessionExitGate()

    // Sessão encerrada pelo Firebase com o app aberto (conta excluída ou desativada, senha trocada
    // ou token revogado em outro aparelho): sem isso, o usuário ficaria no menu com as chamadas
    // recusadas. O próprio Sair passa pelo portão antes do signOut, então não cai aqui.
    private val authStateListener = FirebaseAuth.AuthStateListener { auth ->
        if (exitGate.onAuthStateChanged(isSignedIn = auth.currentUser != null)) {
            Log.i(TAG, "Sessão encerrada fora do app; voltando ao login")
            // Não dá para distinguir conta excluída de desativada: reapresenta o aviso de risco,
            // obrigatório para quem se cadastrar de novo no aparelho (NF-05).
            appPreferences.disclaimerAccepted = false
            leaveToLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Previne voltar para AuthenticationActivity
            }
        })

        setContent {
            ChartPatternTrackerTheme {
                val navController = rememberNavController()

                AuthProviderWrapper(authProvider = FirebaseAuthProvider()) {
                    CompositionLocalProvider(LocalPatternProvider provides FirebasePatternProvider()) {
                        AppNavHost(
                            navController = navController,
                            onLogout = { handleLogout() }
                        )
                    }
                }
            }
        }
    }

    // Só com a tela visível: ao voltar do segundo plano, o Firebase chama o listener na hora com
    // o estado atual, e a troca de tela não acontece com o app em segundo plano.
    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener)
    }

    override fun onStop() {
        FirebaseAuth.getInstance().removeAuthStateListener(authStateListener)
        super.onStop()
    }

    private fun handleLogout() {
        if (exitGate.tryBeginExit()) leaveToLogin()
    }

    private fun leaveToLogin() {
        val auth = FirebaseAuth.getInstance()
        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        )

        // 1. Deslogar do Firebase (sem efeito se a sessão já acabou)
        auth.signOut()

        // 2. Deslogar do Google Sign-In (para forçar a escolha de uma conta ao logar novamente)
        googleSignInClient.signOut().addOnCompleteListener {

            // 3. Redirecionar para a tela de login
            val intent = Intent(this, AuthenticationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private companion object {
        const val TAG = "MainActivity"
    }
}

/**
 * Garante uma só saída da [MainActivity] para o login, venha ela do Sair do usuário ou do fim da
 * sessão pelo Firebase.
 *
 * Os dois caminhos se cruzam: o Sair chama `signOut`, que dispara o `AuthStateListener`, e a
 * exclusão de conta encerra a sessão sozinha antes de avisar a tela. Sem o portão, a tela de login
 * abriria duas vezes.
 */
internal class SessionExitGate {

    private var exiting = false

    /**
     * Pedido de saída do usuário (Sair, Recusar o aviso de risco, conta excluída pelo Perfil).
     *
     * @return `true` se a saída começa agora; `false` se já estava em curso.
     */
    fun tryBeginExit(): Boolean {
        if (exiting) return false
        exiting = true
        return true
    }

    /**
     * Mudança de sessão informada pelo `AuthStateListener`.
     *
     * @return `true` se a sessão acabou sem uma saída em curso, caso em que a saída começa agora.
     */
    fun onAuthStateChanged(isSignedIn: Boolean): Boolean = !isSignedIn && tryBeginExit()
}
