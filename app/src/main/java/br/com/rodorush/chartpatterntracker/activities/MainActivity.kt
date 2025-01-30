package br.com.rodorush.chartpatterntracker.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import br.com.rodorush.chartpatterntracker.ui.screens.MainScreen
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        padding = innerPadding,
                        onLogout = { handleLogout() }
                    )
                }
            }
        }
    }

    private fun handleLogout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, AuthenticationActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}