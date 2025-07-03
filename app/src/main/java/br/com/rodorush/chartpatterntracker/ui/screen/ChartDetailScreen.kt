package br.com.rodorush.chartpatterntracker.ui.screen

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    detectPatterns: Boolean = true,
    onNavigateBack: () -> Unit
) {
    val viewModel: ChartViewModel = koinViewModel() // Deixa o Koin fornecer o ViewModel

    val candlestickData by viewModel.candlestickData.collectAsState()
    val patternsData by viewModel.patternsData.collectAsState()
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

    LaunchedEffect(ticker, timeframe, detectPatterns) {
        viewModel.fetchData(ticker, "3mo", timeframe, detectPatterns)
    }

    LaunchedEffect(candlestickData, patternsData, isChartInitialized) {
        if (candlestickData.isNotEmpty() && isChartInitialized) {
            webViewRef?.let { webView ->
                val highlightTimes = patternsData.flatMap { it.candles }.map { it.time }.toSet()
                val jsonData = candlestickData.toJsonArray(highlightTimes)
                Log.d("ChartDetailScreen", "JSON Data: $jsonData")
                Log.d("ChartDetailScreen", "Updating chart with ${candlestickData.size} candles")
                coroutineScope.launch {
                    webView.evaluateJavascript("updateChart('$jsonData')", null)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Vazio, apenas o ícone de voltar */ },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("ChartDetailScreen", "UI Back button clicked")
                        onNavigateBack()
                    }) {
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
                    .padding(paddingValues) // Respeita o espaço da TopAppBar
            ) {
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
                                    return !url.startsWith("file:///android_asset/")
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
        }
    )

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

fun List<Candlestick>.toJsonArray(highlightTimes: Set<Long> = emptySet()): String {
    val jsonArray = JSONArray()
    for (item in this) {
        val jsonObject = JSONObject().apply {
            put("time", item.time)
            put("open", item.open)
            put("high", item.high)
            put("low", item.low)
            put("close", item.close)
            put("volume", item.volume ?: 0)
            if (item.time in highlightTimes) {
                if (item.close > item.open) {
                    // Bullish candle of the Harami pattern - white body with blue borders
                    put("color", "#4CC9F0")
                    put("wickColor", "#0000FF")
                    put("borderColor", "#0000FF")
                } else {
                    // Bearish candle remains solid blue
                    put("color", "#0000FF")
                    put("wickColor", "#0000FF")
                    put("borderColor", "#0000FF")
                }
            }
        }
        jsonArray.put(jsonObject)
    }
    return jsonArray.toString().replace("\"", "\\\"")
}