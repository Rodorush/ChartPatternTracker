package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.ChartInterval
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: ChartInterval,
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = viewModel()
) {
    val candlestickData by viewModel.candlestickData.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isChartInitialized by remember { mutableStateOf(false) }

    // Fetch data when ticker or timeframe changes
    LaunchedEffect(ticker, timeframe) {
        viewModel.fetchData(ticker, "3mo", timeframe)
    }

    // Update chart when candlestick data changes and chart is initialized
    LaunchedEffect(candlestickData, isChartInitialized) {
        if (candlestickData.isNotEmpty() && isChartInitialized) {
            webViewRef?.let { webView ->
                val jsonData = candlestickData.toJsonArray()
                Log.d("ChartDetailScreen", "JSON Data: $jsonData")
                Log.d("ChartDetailScreen", "Updating chart with ${candlestickData.size} candles")
                webView.evaluateJavascript("updateChart('$jsonData')", null)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Back button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        // Display loading indicator
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Display error if present
        if (error != null) {
            Text(
                text = "Error: $error",
                modifier = Modifier.align(Alignment.Center)
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewRef = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    addJavascriptInterface(WebAppInterface(this) { isChartInitialized = true }, "AndroidInterface")
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            if (url.startsWith("file:///android_asset/")) {
                                return false // Allow loading
                            }
                            return true // Block all other URLs
                        }
                    }
                    loadUrl("file:///android_asset/chart.html")
                }
            }
        )
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                webView.stopLoading()
                webView.clearCache(true)
                webView.clearHistory()
                webView.loadUrl("about:blank")
                webView.destroy()
                webViewRef = null
                Log.d("ChartDetailScreen", "WebView destroyed successfully")
            }
        }
    }
}

// JavaScript Interface
class WebAppInterface(
    private val webView: WebView,
    private val onChartInitialized: () -> Unit
) {
    @JavascriptInterface
    fun chartInitialized() {
        onChartInitialized()
    }

    @JavascriptInterface
    fun updateChart(jsonData: String) {
        webView.post {
            webView.evaluateJavascript("""
                (function() {
                    if (!chart || !candlestickSeries) {
                        console.error("Chart not initialized. Initializing now...");
                        initializeChart();
                    }
                    const data = JSON.parse('$jsonData');
                    candlestickSeries.setData(data);
                    chart.timeScale().fitContent();
                })();
            """.trimIndent(), null)
        }
    }
}

// Extension function to convert candlestick data to JSON
fun List<Candlestick>.toJsonArray(): String {
    val jsonArray = JSONArray()
    for (item in this) {
        val jsonObject = JSONObject().apply {
            put("time", item.time)
            put("open", item.open)
            put("high", item.high)
            put("low", item.low)
            put("close", item.close)
            put("volume", item.volume ?: 0) // Mantido por compatibilidade, mas ignorado no gráfico
        }
        jsonArray.put(jsonObject)
    }
    return jsonArray.toString().replace("\"", "\\\"")
}