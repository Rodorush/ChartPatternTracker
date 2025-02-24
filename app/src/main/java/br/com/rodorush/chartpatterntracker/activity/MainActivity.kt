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
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider
import br.com.rodorush.chartpatterntracker.util.provider.FirebaseAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.FirebasePatternProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
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

    private fun handleLogout() {
        val auth = FirebaseAuth.getInstance()
        val googleSignInClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        )

        // 1. Deslogar do Firebase
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
}