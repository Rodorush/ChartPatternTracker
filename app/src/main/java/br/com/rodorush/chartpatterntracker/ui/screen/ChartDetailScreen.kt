package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.view.ChartsView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = viewModel()
) {
    val candlestickData by viewModel.candlestickData.collectAsState()
    val currentSource by viewModel.currentSource.collectAsState()
    val seriesApi = remember { mutableStateOf<SeriesApi?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Brapi") }
    var isDropdownExpanded by remember { mutableStateOf(false) } // Estado do dropdown

    LaunchedEffect(ticker, timeframe, currentSource) {
        viewModel.fetchData(ticker, "1mo", timeframe)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dropdown para selecionar fonte
        Row(modifier = Modifier.padding(16.dp)) {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSource,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fonte de Dados") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .weight(1f)
                )

                // Menu Dropdown ancorado corretamente
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    listOf("Brapi", "Mock", "AlphaVantage").forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source) },
                            onClick = {
                                selectedSource = source
                                viewModel.setDataSource(source)
                                isDropdownExpanded = false // Fechar o menu
                            }
                        )
                    }
                }
            }
        }

        // Campo para chave de API, se necessário
        if (currentSource.requiresApiKey()) {
            Row(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Chave da API") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.setApiKey(apiKeyInput)
                        viewModel.fetchData(ticker, "1mo", timeframe)
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Aplicar")
                }
            }
        }

        // Gráfico
        AndroidView(
            modifier = Modifier.weight(1f),
            factory = { context ->
                ChartsView(context).apply {
                    val chartApi = this.api
                    chartApi.addCandlestickSeries(
                        options = CandlestickSeriesOptions().apply {
                            upColor = IntColor("#26A69A".toColorInt())
                            downColor = IntColor("#EF5350".toColorInt())
                        },
                        onSeriesCreated = { createdSeries ->
                            seriesApi.value = createdSeries
                        }
                    )
                }
            },
            update = {
                seriesApi.value?.let { series ->
                    if (candlestickData.isNotEmpty()) {
                        Log.d("ChartDetailScreen", "Atualizando série com ${candlestickData.size} candles")
                        series.setData(candlestickData)
                    }
                }
            }
        )
    }
}