package br.com.rodorush.chartpatterntracker.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.component.LegalAgreementText

/**
 * Aviso de risco financeiro (NF-05).
 *
 * - [readOnly] = false: gate de primeiro uso, com botão "Li e concordo" que dispara [onAccept]
 *   e botão "Não aceito" que dispara [onDecline] (recusa o aviso e aborta a sessão via logout).
 * - [readOnly] = true: modo leitura (acessível por Configurações), só com "Voltar" ([onNavigateBack]).
 */
@Composable
fun DisclaimerScreen(
    readOnly: Boolean,
    onAccept: () -> Unit = {},
    onDecline: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = stringResource(R.string.logo),
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.disclaimer_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.disclaimer_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Aceite de Termos/Política embutido no gate universal: cobre também quem entra
        // via Google (que não passa pelo checkbox do cadastro por e-mail). O "Li e concordo"
        // abaixo vale para o aviso de risco + Termos/Política; persistido em disclaimerAccepted.
        LegalAgreementText(
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (readOnly) {
            TextButton(onClick = onNavigateBack) {
                Text(text = stringResource(R.string.back))
            }
        } else {
            Button(
                onClick = onAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(text = stringResource(R.string.disclaimer_accept))
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.disclaimer_decline))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DisclaimerScreenGatePreview() {
    DisclaimerScreen(readOnly = false)
}

@Preview(showBackground = true)
@Composable
private fun DisclaimerScreenReadOnlyPreview() {
    DisclaimerScreen(readOnly = true)
}
