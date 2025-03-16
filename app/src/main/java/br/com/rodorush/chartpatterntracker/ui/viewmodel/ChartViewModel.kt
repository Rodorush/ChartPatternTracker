package br.com.rodorush.chartpatterntracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.data.AlphaVantageDataSource
import br.com.rodorush.chartpatterntracker.data.BrapiDataSource
import br.com.rodorush.chartpatterntracker.data.MockDataSource
import br.com.rodorush.chartpatterntracker.data.AssetDataSource
import com.tradingview.lightweightcharts.api.series.models.CandlestickData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChartViewModel : ViewModel() {
    private val _candlestickData = MutableStateFlow<List<CandlestickData>>(emptyList())
    val candlestickData: StateFlow<List<CandlestickData>> = _candlestickData

    private val _currentSource = MutableStateFlow<AssetDataSource>(BrapiDataSource())
    val currentSource: StateFlow<AssetDataSource> = _currentSource

    private val sources = mapOf(
        "Brapi" to BrapiDataSource(),
        "Mock" to MockDataSource(),
        "AlphaVantage" to AlphaVantageDataSource()
    )

    init {
        _currentSource.value.setApiKey(BuildConfig.BRAPI_TOKEN)
    }

    fun setDataSource(sourceName: String) {
        _currentSource.value = sources[sourceName] ?: return
    }

    fun setApiKey(apiKey: String) {
        _currentSource.value.setApiKey(apiKey)
    }

    fun fetchData(ticker: String, range: String, interval: String) {
        viewModelScope.launch {
            val result = _currentSource.value.getHistoricalData(ticker, range, interval)
            result.onSuccess { data ->
                _candlestickData.value = data
            }.onFailure { e ->
                _candlestickData.value = emptyList()
                // Log ou UI para erro pode ser adicionado
            }
        }
    }
}