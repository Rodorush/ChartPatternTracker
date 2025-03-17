package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.view.ChartsView

@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = viewModel()
) {
    val candlestickData by viewModel.candlestickData.collectAsState()
    val seriesApi = remember { mutableStateOf<SeriesApi?>(null) }

    LaunchedEffect(ticker, timeframe) {
        viewModel.fetchData(ticker, "1mo", timeframe)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
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