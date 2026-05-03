package br.com.rodorush.chartpatterntracker.ui.screen

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.MaterialTheme
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
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.viewmodel.ChartViewModel
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import br.com.rodorush.chartpatterntracker.util.LocalPatternProvider
import br.com.rodorush.customlogs.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

private const val TAG = "ChartDetailScreen"

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    patternId: String? = null,
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
    val patternProvider = LocalPatternProvider.current
    var patternName by remember { mutableStateOf<String?>(null) }

    // Gerenciar o botão Back do Android
    BackHandler(enabled = true) {
        Log.d(TAG, "Android Back button pressed, calling onNavigateBack")
        onNavigateBack()
    }

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

    LaunchedEffect(candlestickData, patternsData, isChartInitialized) {
        if (candlestickData.isNotEmpty() && isChartInitialized) {
            webViewRef?.let { webView ->
                // Construir intervalos de destaque por ocorrência
                val occurrences = if (patternId != null) {
                    patternsData[patternId].orEmpty()
                } else {
                    patternsData.values.flatten()
                }
                val step = timeframeToMillis(timeframe)
                val highlightRanges = occurrences.mapNotNull { occ ->
                    val times = occ.candles.map { it.time }.sorted()
                    if (times.isEmpty()) null else {
                        val from = times.first()
                        val last = times.last()
                        val to = last + step
                        val minPrice = occ.candles.minOf { it.low.toDouble() }.toFloat()
                        val maxPrice = occ.candles.maxOf { it.high.toDouble() }.toFloat()
                        mapOf(
                            "type" to "REGULAR",
                            "from" to from,
                            "to" to to,
                            "min" to minPrice,
                            "max" to maxPrice,
                            "border" to mapOf("left" to true, "right" to true),
                            "label" to mapOf(
                                "text" to (patternName ?: patternId ?: "Pattern"),
                                "placement" to "right-right"
                            )
                        )
                    }
                }

                val jsonData = candlestickData.toChartPayload(highlightRanges)
                Log.d(TAG, "Updating chart with ${candlestickData.size} candles and ${highlightRanges.size} highlights")
                coroutineScope.launch {
                    delay(50)
                    webView.evaluateJavascript("updateChart('$jsonData')", null)
                }
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
                    IconButton(onClick = {
                        Log.d(TAG, "UI Back button clicked")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                        modifier = Modifier.matchParentSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                // Assegura LayoutParams preenchendo o contêiner
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                addJavascriptInterface(WebAppInterface(this) { isChartInitialized = true }, "AndroidInterface")
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        val url = request.url.toString()
                                        Log.d(TAG, "WebView loading URL: $url")
                                        return !url.startsWith("file:///android_asset/")
                                    }

                                    override fun onPageFinished(view: WebView, url: String) {
                                        Log.d(TAG, "WebView page finished: $url")
                                        // Força a inicialização do gráfico caso o onload não dispare
                                        view.evaluateJavascript(
                                            (
                                                """
                                                (function(){
                                                    try {
                                                        console.log('[Android] onPageFinished: attempting initializeChart');
                                                        if (typeof initializeChart === 'function') {
                                                            initializeChart();
                                                        } else {
                                                            console.warn('[Android] initializeChart is not defined yet');
                                                        }
                                                        var el = document.getElementById('chart');
                                                        if (el) {
                                                            var r = el.getBoundingClientRect();
                                                            console.log('[Android] chart container size after finish:', r.width + 'x' + r.height);
                                                        }
                                                    } catch (e) {
                                                        console.error('[Android] initializeChart error:', e);
                                                    }
                                                })();
                                                """
                                                ).trimIndent(),
                                            null
                                        )
                                    }
                                }
                                loadUrl("file:///android_asset/chart.html")
                                Log.d(TAG, "WebView created and loading chart.html")
                            }
                        },
                        update = { /* Evitar recarga */ }
                    )
                }
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                coroutineScope.launch(Dispatchers.Main) {
                    try {
                        Log.d(TAG, "Starting WebView cleanup")
                        webView.stopLoading()
                        webView.pauseTimers()
                        webView.onPause()
                        webView.removeJavascriptInterface("AndroidInterface")
                        webView.clearCache(true)
                        webView.clearHistory()
                        webView.removeAllViews()
                        Log.d(TAG, "WebView cleanup completed, delaying destroy")
                        delay(100)
                        webView.destroy()
                        Log.d(TAG, "WebView destroyed successfully")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during WebView cleanup: $e")
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
            Log.d(TAG, "JavaScript: chartInitialized called")
            onChartInitialized()
        } else {
            Log.w(TAG, "JavaScript: chartInitialized ignored, WebView destroyed")
        }
    }

    @JavascriptInterface
    fun updateChart(jsonData: String) {
        if (!isDestroyed) {
            Log.d(TAG, "JavaScript: updateChart called")
            webView.post {
                if (!isDestroyed) {
                    webView.evaluateJavascript(
                        """
                        (function() {
                            if (!chart) {
                                console.error('Chart not initialized. Initializing now...');
                                initializeChart();
                            }
                            const data = JSON.parse('$jsonData');
                            chart.setData({ candles: data });
                            chart.setAutoScale(true);
                        })();
                        """.trimIndent(), null
                    )
                } else {
                    Log.w(TAG, "JavaScript: updateChart ignored, WebView destroyed")
                }
            }
        } else {
            Log.w(TAG, "JavaScript: updateChart ignored, WebView destroyed")
        }
    }
}

fun List<Candlestick>.toChartPayload(highlights: List<Map<String, Any?>> = emptyList()): String {
    val candlesArray = JSONArray()
    for (item in this) {
        val jsonObject = JSONObject().apply {
            // DXCharts Lite expects keys: timestamp, open, close, hi, lo, volume
            put("timestamp", item.time)
            put("open", item.open)
            put("close", item.close)
            put("hi", item.high)
            put("lo", item.low)
            put("volume", item.volume ?: 0)
        }
        candlesArray.put(jsonObject)
    }

    val payload = JSONObject().apply {
        put("candles", candlesArray)
        if (highlights.isNotEmpty()) {
            val highlightsArray = JSONArray()
            for (h in highlights) {
                val hObj = JSONObject()
                h["type"]?.let { hObj.put("type", it) }
                h["from"]?.let { hObj.put("from", it) }
                h["to"]?.let { hObj.put("to", it) }
                (h["label"] as? Map<*, *>)?.let { labelMap ->
                    val lObj = JSONObject()
                    labelMap["text"]?.let { lObj.put("text", it) }
                    labelMap["placement"]?.let { lObj.put("placement", it) }
                    hObj.put("label", lObj)
                }
                highlightsArray.put(hObj)
            }
            put("highlights", highlightsArray)
        }
    }
    // Escape quotes to safely pass as a single-quoted JS string literal
    return payload.toString().replace("\"", "\\\"")
}

private fun timeframeToMillis(tf: String): Long = when (tf.lowercase(Locale.getDefault())) {
    "1m", "1min", "1minuto" -> 60_000L
    "5m" -> 5 * 60_000L
    "15m" -> 15 * 60_000L
    "30m" -> 30 * 60_000L
    "1h", "1hr", "1hora" -> 60 * 60_000L
    "4h" -> 4 * 60 * 60_000L
    "1d", "1dia" -> 24 * 60 * 60_000L
    "1wk", "1w", "1sem" -> 7 * 24 * 60 * 60_000L
    "1mo", "1mês", "1mes" -> 30L * 24 * 60 * 60_000L
    else -> 24 * 60 * 60_000L // fallback
}
