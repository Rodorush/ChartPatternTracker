package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import android.webkit.WebView
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
    var chartsViewRef by remember { mutableStateOf<ChartsView?>(null) }

    LaunchedEffect(ticker, timeframe) {
        viewModel.fetchData(ticker, "3mo", timeframe)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ChartsView(context).apply {
                chartsViewRef = this
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
        update = { chartsView ->
            seriesApi.value?.let { series ->
                if (candlestickData.isNotEmpty()) {
                    Log.d("ChartDetailScreen", "Atualizando série com ${candlestickData.size} candles")
                    series.setData(candlestickData)
                }
            }
        }
    )

    // Limpeza ao sair do composable
    DisposableEffect(Unit) {
        onDispose {
            chartsViewRef?.let { chartsView ->
                try {
                    // Acessar a WebView interna
                    val webViewField = ChartsView::class.java.getDeclaredField("webView")
                    webViewField.isAccessible = true
                    val webView = webViewField.get(chartsView) as WebView

                    // Limpar e destruir a WebView
                    webView.stopLoading()
                    webView.clearCache(true)
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.loadUrl("about:blank")
                    webView.destroy()

                    Log.d("ChartDetailScreen", "WebView destruída com sucesso")
                } catch (e: Exception) {
                    Log.e("ChartDetailScreen", "Erro ao destruir WebView: ${e.message}")
                }
                chartsViewRef = null
                seriesApi.value = null
            }
        }
    }
}