package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.ui.theme.ChartPatternTrackerTheme
import br.com.rodorush.chartpatterntracker.viewmodel.ScreeningViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreeningResultsScreen(
    viewModel: ScreeningViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onCardClick: (String, String, String) -> Unit = { _, _, _ -> }
) {
    Log.d("ScreeningResultsScreen", "Tela ScreeningResultsScreen carregada")
    // Coleta os resultados do ViewModel
    val screeningResults by viewModel.screeningResults.collectAsState()
    val isScreening by viewModel.isScreening.collectAsState()
    val shouldRefresh by viewModel.shouldRefresh.collectAsState()

    // Inicia a busca automaticamente
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh || screeningResults.isEmpty()) {
            Log.d("ScreeningResultsScreen", "Iniciando busca com startScreening")
            viewModel.startScreening()
        } else {
            Log.d("ScreeningResultsScreen", "Resultados em cache, evitando nova busca")
        }
    }

    // Estado para o campo de busca
    var searchText by remember { mutableStateOf("") }

    BackHandler(enabled = true) {
        viewModel.cancelScreening()
        onNavigateBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelScreening()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Ação do perfil */ }) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.profile)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text(stringResource(R.string.search_patterns_found)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = stringResource(R.string.search)
                                )
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.patterns_found_completed),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (screeningResults.isEmpty()) {
                if (isScreening) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Text(
                        text = stringResource(id = R.string.no_patterns_found),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(screeningResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCardClick(result.asset.ticker, result.timeframe.value, result.pattern.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Miniatura do padrão (placeholder)
                                Image(
                                    painter = painterResource(id = R.drawable.castical_64px),
                                    contentDescription = stringResource(R.string.pattern_description),
                                    modifier = Modifier.width(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(result.timeframe.nameRes),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = result.asset.ticker,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = result.pattern.getLocalized("name"),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Confiabilidade: ${result.reliability}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // Indicação com ícone em coluna
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        painter = painterResource(id = result.indicationIcon),
                                        contentDescription = result.indication,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Text(
                                        text = result.indication,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
                if (isScreening) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScreeningResultsScreenPreview() {
    ChartPatternTrackerTheme {
        ScreeningResultsScreen(onCardClick = { _, _, _ -> })
    }
}