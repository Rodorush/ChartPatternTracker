package br.com.rodorush.chartpatterntracker.ui.screen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.component.AuthProviderWrapper
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider

@Composable
fun MainScreen(
    onNavigateToSelectChartPattern: () -> Unit,
    onNavigateToRealTimeQuotes: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(100.dp))
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.main_menu),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(20.dp))
        ButtonMenuItem(
            R.string.pattern_tracker,
            onClick = onNavigateToSelectChartPattern
        )
        ButtonMenuItem(R.string.financial_news, comingSoon = true)
        ButtonMenuItem(R.string.real_time_quotes, onClick = onNavigateToRealTimeQuotes)
        ButtonMenuItem(R.string.technical_indicators, comingSoon = true)
        ButtonMenuItem(R.string.settings, onClick = onNavigateToSettings)
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = {
            // O signOut fica com a MainActivity, depois do portão de saída: chamado aqui antes,
            // o AuthStateListener poderia tratar o Sair como sessão encerrada fora do app.
            Toast.makeText(context,
                context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
            onLogout()
        }) {
            Text(text = stringResource(R.string.logout))
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        }
    }
}

/**
 * Item do menu principal.
 *
 * @param comingSoon `true` para recurso ainda não entregue: o botão fica desativado, ignora o
 * [onClick] e mostra o selo "Em breve" ao lado do rótulo.
 */
@Composable
fun ButtonMenuItem(
    texResId: Int,
    onClick: () -> Unit = {},
    comingSoon: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = !comingSoon,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp)
    ) {
        Text(text = stringResource(texResId))
        if (comingSoon) {
            Spacer(modifier = Modifier.width(8.dp))
            ComingSoonBadge()
        }
    }
}

/** Selo "Em breve". Tem cor própria, para não esmaecer junto com o botão desativado. */
@Composable
private fun ComingSoonBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.coming_soon),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    ChartPatternTrackerTheme {
        AuthProviderWrapper(authProvider = MockAuthProvider()) {
            ChartPatternTrackerTheme {
                MainScreen(
                    onNavigateToSelectChartPattern = {},
                    onNavigateToRealTimeQuotes = {},
                    onLogout = {})
            }
        }
    }
}