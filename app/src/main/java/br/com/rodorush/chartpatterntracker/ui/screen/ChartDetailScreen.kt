package br.com.rodorush.chartpatterntracker.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider
import io.data2viz.charts.chart.chart
import io.data2viz.charts.chart.mark.MarkCurves
import io.data2viz.charts.chart.mark.line
import io.data2viz.charts.chart.quantitative
import io.data2viz.charts.chart.temporal
import io.data2viz.viz.VizContainer
import io.data2viz.viz.VizContainerView
import kotlinx.datetime.Instant
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    patternId: String? = null,
    detectPatterns: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChartViewModel = koinViewModel()
    val candlestickData by viewModel.candlestickData.collectAsState()
    val patternsData by viewModel.patternsData.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val patternProvider = LocalPatternProvider.current
    var patternName by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) { onNavigateBack() }

    LaunchedEffect(ticker, timeframe, patternId, detectPatterns) {
        val ids = if (detectPatterns) {
            patternId?.let { listOf(it) } ?: br.com.rodorush.chartpatterntracker.model.pattern.PatternDetectorRegistry.registeredIds()
        } else emptyList()
        viewModel.fetchData(ticker, "3mo", timeframe, detectPatterns, ids)
        patternName = null
        patternId?.let { id ->
            patternProvider.fetchPatternById(id) { pattern ->
                patternName = pattern?.getLocalized("name")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val timeframeName = TimeframeItem.availableTimeframes
                        .firstOrNull { it.value.equals(timeframe, ignoreCase = true) }
                        ?.let { stringResource(it.nameRes) }
                        ?: timeframe.uppercase()
                    Column {
                        Text(
                            text = stringResource(
                                R.string.chart_detail_title,
                                ticker.uppercase(),
                                timeframeName
                            ),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        patternName?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    error != null -> Text("Error: $error", modifier = Modifier.align(Alignment.Center))
                    candlestickData.isNotEmpty() -> Viz(modifier = Modifier.fillMaxSize()) {
                        it.candlestickLineChart(candlestickData)
                    }
                }
            }
        }
    )
}

@Composable
private fun Viz(modifier: Modifier = Modifier, block: (VizContainer) -> Unit) {
    AndroidView(
        factory = { VizContainerView(it) },
        modifier = modifier,
        update = { block(it) }
    )
}

private fun VizContainer.candlestickLineChart(data: List<Candlestick>) {
    chart(data) {
        val time = temporal({ Instant.fromEpochMilliseconds(domain.time) }) {
            name = "Time"
        }
        val price = quantitative({ domain.close.toDouble() }) {
            name = "Close"
        }
        line(time, price) {
            curve = MarkCurves.Straight
        }
    }
}
