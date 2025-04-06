package br.com.rodorush.chartpatterntracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.data.AlphaVantageDataSource
import br.com.rodorush.chartpatterntracker.data.BrapiDataSource
import br.com.rodorush.chartpatterntracker.data.MockDataSource
import br.com.rodorush.chartpatterntracker.data.AssetDataSource
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.ChartInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChartViewModel : ViewModel() {
    private val _candlestickData = MutableStateFlow<List<Candlestick>>(emptyList())
    val candlestickData: StateFlow<List<Candlestick>> = _candlestickData

    private val _currentSource = MutableStateFlow<AssetDataSource>(BrapiDataSource())
    val currentSource: StateFlow<AssetDataSource> = _currentSource

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

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

    fun fetchData(ticker: String, range: String, interval: ChartInterval) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = _currentSource.value.getHistoricalData(ticker, range, interval.value)
            result.onSuccess { data ->
                _candlestickData.value = data
                _error.value = null
            }.onFailure { e ->
                _candlestickData.value = emptyList()
                _error.value = e.message ?: "Failed to fetch data"
            }
            _isLoading.value = false
        }
    }

    // Novo método para serializar os dados em JSON
    fun getCandlestickDataAsJson(): String {
        val jsonArray = JSONArray()
        _candlestickData.value.forEach { candlestick ->
            val jsonObject = JSONObject().apply {
                put("time", candlestick.time) // Mantém como número (milissegundos)
                put("open", candlestick.open)
                put("high", candlestick.high)
                put("low", candlestick.low)
                put("close", candlestick.close)
                candlestick.volume?.let { put("volume", it) }
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
}