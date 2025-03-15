package br.com.rodorush.chartpatterntracker.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import com.tradingview.lightweightcharts.api.chart.models.color.IntColor
import com.tradingview.lightweightcharts.api.interfaces.ChartApi
import com.tradingview.lightweightcharts.api.interfaces.SeriesApi
import com.tradingview.lightweightcharts.api.options.models.CandlestickSeriesOptions
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import com.tradingview.lightweightcharts.api.series.models.Time
import com.tradingview.lightweightcharts.view.ChartsView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Composable
fun ChartDetailScreen(
    ticker: String,
    timeframe: String,
    onNavigateBack: () -> Unit
) {
    var candlestickData by remember { mutableStateOf<List<CandlestickData>>(emptyList()) }
    val seriesApi = remember { mutableStateOf<SeriesApi?>(null) }

    LaunchedEffect(ticker, timeframe) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://brapi.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = retrofit.create(BrapiApiService::class.java)
        Log.d("ChartDetailScreen", "Ticker: $ticker, Timeframe: $timeframe")

        try {
            val response = service.getHistoricalData(
                ticker = ticker,
                range = "5d",
                interval = timeframe,
                token = BuildConfig.BRAPI_TOKEN
            )
            Log.d("ChartDetailScreen", "Resposta bem-sucedida: $response")
            val historicalData = response.results.firstOrNull()?.historicalDataPrice?.map { data ->
                CandlestickData(
                    time = Time.Utc(data.date / 1000),
                    open = data.open.toFloat(),
                    high = data.high.toFloat(),
                    low = data.low.toFloat(),
                    close = data.close.toFloat()
                )
            } ?: emptyList()
            candlestickData = historicalData
        } catch (e: Exception) {
            Log.e("ChartDetailScreen", "Erro ao buscar dados: ${e.message}")
            candlestickData = emptyList()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            ChartsView(context).apply {
                val chartApi: ChartApi = this.api
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
                    Log.d("ChartDetailScreen", "Atualizando série com: $candlestickData.size dados")
                    series.setData(candlestickData)
                }
            }
        }
    )
}