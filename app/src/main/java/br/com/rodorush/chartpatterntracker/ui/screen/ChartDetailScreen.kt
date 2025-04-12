package br.com.rodorush.chartpatterntracker.ui.screen

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.ChartInterval
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: ChartInterval,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("chart_prefs", Context.MODE_PRIVATE) }
    val viewModel: ChartViewModel = viewModel(
        factory = ChartViewModelFactory(preferences)
    )

    val candlestickData by viewModel.candlestickData.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isChartInitialized by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Gerenciar o botão Back do Android
    BackHandler(enabled = true) {
        Log.d("ChartDetailScreen", "Android Back button pressed, calling onNavigateBack")
        onNavigateBack()
    }

    LaunchedEffect(ticker, timeframe) {
        viewModel.fetchData(ticker, "3mo", timeframe)
    }

    LaunchedEffect(candlestickData, isChartInitialized) {
        if (candlestickData.isNotEmpty() && isChartInitialized) {
            webViewRef?.let { webView ->
                val jsonData = candlestickData.toJsonArray()
                Log.d("ChartDetailScreen", "JSON Data: $jsonData")
                Log.d("ChartDetailScreen", "Updating chart with ${candlestickData.size} candles")
                coroutineScope.launch {
                    webView.evaluateJavascript("updateChart('$jsonData')", null)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = {
                Log.d("ChartDetailScreen", "UI Back button clicked")
                onNavigateBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

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
                            Log.d("ChartDetailScreen", "WebView loading URL: $url")
                            if (url.startsWith("file:///android_asset/")) {
                                return false
                            }
                            return true
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            Log.d("ChartDetailScreen", "WebView page finished: $url")
                        }
                    }
                    loadUrl("file:///android_asset/chart.html")
                    Log.d("ChartDetailScreen", "WebView created and loading chart.html")
                }
            },
            update = { /* Evitar recarga */ }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        Log.d("ChartDetailScreen", "Starting WebView cleanup")
                        webView.stopLoading()
                        webView.pauseTimers()
                        webView.onPause()
                        webView.removeJavascriptInterface("AndroidInterface")
                        webView.clearCache(true)
                        webView.clearHistory()
                        webView.removeAllViews()
                        Log.d("ChartDetailScreen", "WebView cleanup completed, delaying destroy")
                        // Pequeno atraso para permitir que o Chromium processe eventos pendentes
                        delay(100)
                        webView.destroy()
                        Log.d("ChartDetailScreen", "WebView destroyed successfully")
                    } catch (e: Exception) {
                        Log.e("ChartDetailScreen", "Error during WebView cleanup: $e")
                    } finally {
                        webViewRef = null
                    }
                }
            }
        }
    }
}

class WebAppInterface(
    private val webView: WebView,
    private val onChartInitialized: () -> Unit
) {
    private var isDestroyed by mutableStateOf(false)

    @JavascriptInterface
    fun chartInitialized() {
        if (!isDestroyed) {
            Log.d("ChartDetailScreen", "JavaScript: chartInitialized called")
            onChartInitialized()
        } else {
            Log.w("ChartDetailScreen", "JavaScript: chartInitialized ignored, WebView destroyed")
        }
    }

    @JavascriptInterface
    fun updateChart(jsonData: String) {
        if (!isDestroyed) {
            Log.d("ChartDetailScreen", "JavaScript: updateChart called")
            webView.post {
                if (!isDestroyed) {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            if (!chart || !candlestickSeries) {
                                console.error("Chart not initialized. Initializing now...");
                                initializeChart();
                            }
                            const data = JSON.parse('$jsonData');
                            candlestickSeries.setData(data);
                            chart.timeScale().fitContent();
                        })();
                        """.trimIndent(), null
                    )
                } else {
                    Log.w("ChartDetailScreen", "JavaScript: updateChart ignored, WebView destroyed")
                }
            }
        } else {
            Log.w("ChartDetailScreen", "JavaScript: updateChart ignored, WebView destroyed")
        }
    }
}

fun List<Candlestick>.toJsonArray(): String {
    val jsonArray = JSONArray()
    for (item in this) {
        val jsonObject = JSONObject().apply {
            put("time", item.time)
            put("open", item.open)
            put("high", item.high)
            put("low", item.low)
            put("close", item.close)
            put("volume", item.volume ?: 0)
        }
        jsonArray.put(jsonObject)
    }
    return jsonArray.toString().replace("\"", "\\\"")
}