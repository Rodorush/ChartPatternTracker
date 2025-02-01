package br.com.rodorush.chartpatterntracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.rodorush.chartpatterntracker.navigation.Screen
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectChartPatternScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Padrões Gráficos") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Selecione os padrões gráficos que deseja rastrear:")

            // Exemplo (meramente ilustrativo):
            // - Uma lista de checkboxes, por ex.
            // - ...

            Spacer(modifier = Modifier.size(24.dp))
            Button(onClick = onNavigateBack) {
                Text(text = "Voltar")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SelectChartPatternPreview() {
    ChartPatternTrackerTheme {
        SelectChartPatternScreen(
            onNavigateBack = {}
        )
    }
}
