package br.com.rodorush.chartpatterntracker.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = viewModel()
) {
    val currentSource by viewModel.currentSource.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Brapi") }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 32.dp)
        )

        // Dropdown para selecionar fonte
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                value = selectedSource,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.data_source)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf("Brapi", "Mock", "AlphaVantage").forEach { source ->
                    DropdownMenuItem(
                        text = { Text(source) },
                        onClick = {
                            selectedSource = source
                            viewModel.setDataSource(source)
                            expanded = false
                        }
                    )
                }
            }
        }

        // Campo para chave de API, se necessário
        if (currentSource.requiresApiKey()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(stringResource(R.string.api_key)) },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.setApiKey(apiKeyInput) },
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(stringResource(R.string.apply))
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 32.dp)
        ) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
@Preview(showBackground = true)
fun SettingsScreenPreview() {
    SettingsScreen(onNavigateBack = {})
}