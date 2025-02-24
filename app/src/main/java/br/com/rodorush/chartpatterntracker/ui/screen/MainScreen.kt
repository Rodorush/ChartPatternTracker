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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import br.com.rodorush.chartpatterntracker.util.LocalAuthProvider
import br.com.rodorush.chartpatterntracker.util.provider.mock.MockAuthProvider

@Composable
fun MainScreen(
    onNavigateToSelectChartPattern: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val authProvider = LocalAuthProvider.current

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
        ButtonMenuItem(R.string.financial_news,
            onClick = {
                Toast.makeText(
                    context,
                    R.string.financial_news,
                    Toast.LENGTH_SHORT
                ).show()
            })
        ButtonMenuItem(R.string.real_time_quotes, onClick = {
            Toast.makeText(
                context,
                R.string.real_time_quotes,
                Toast.LENGTH_SHORT
            ).show()
        })
        ButtonMenuItem(R.string.technical_indicators, onClick = {
            Toast.makeText(
                context,
                R.string.technical_indicators,
                Toast.LENGTH_SHORT
            ).show()
        })
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = {
            authProvider.logout {
                Toast.makeText(context,
                    context.getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                onLogout()
            }
        }) {
            Text(text = stringResource(R.string.logout))
            Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        }
    }
}

@Composable
fun ButtonMenuItem(
    texResId: Int,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = 8.dp)
    ) {
        Text(text = stringResource(texResId))
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
                    onLogout = {})
            }
        }
    }
}