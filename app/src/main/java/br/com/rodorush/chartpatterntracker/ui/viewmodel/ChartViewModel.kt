package br.com.rodorush.chartpatterntracker.ui.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.data.AlphaVantageDataSource
import br.com.rodorush.chartpatterntracker.data.BrapiDataSource
import br.com.rodorush.chartpatterntracker.data.MockDataSource
import br.com.rodorush.chartpatterntracker.data.AssetDataSource
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.CandlestickRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import androidx.core.content.edit

class ChartViewModel(
    private val preferences: SharedPreferences,
    private val repository: CandlestickRepository
) : ViewModel() {
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
        val savedSource = preferences.getString("data_source", "Brapi") ?: "Brapi"
        setDataSource(savedSource)
        _currentSource.value.setApiKey(BuildConfig.BRAPI_TOKEN)
    }

    fun setDataSource(sourceName: String) {
        val newSource = sources[sourceName] ?: return
        _currentSource.value = newSource
        preferences.edit { putString("data_source", sourceName) }
    }

    fun setApiKey(apiKey: String) {
        _currentSource.value.setApiKey(apiKey)
    }

    fun fetchData(ticker: String, range: String, interval: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("ChartViewModel", "Fetching data from source: ${_currentSource.value.javaClass.simpleName}")
                Log.d("ChartViewModel", "Ticker: $ticker, Range: $range, Interval: $interval")
                val initialCandlesticks = _currentSource.value.getHistoricalData(ticker, range, interval)
                initialCandlesticks.onSuccess {
                    val fullCandlesticks = repository.fetchCandlesticks(ticker, interval, range)
                    val haramiCandlesticks = repository.detectHaramiAlta(fullCandlesticks)
                    _candlestickData.value = haramiCandlesticks
                    _error.value = null
                    Log.d("ChartViewModel", "Data fetched successfully: ${haramiCandlesticks.size} candles with Harami Alta")
                }.onFailure { e ->
                    _candlestickData.value = emptyList()
                    _error.value = e.message ?: "Failed to fetch data"
                    Log.e("ChartViewModel", "Error fetching data: $e")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected error"
                Log.e("ChartViewModel", "Unexpected error: $e")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCandlestickDataAsJson(): String {
        val jsonArray = JSONArray()
        _candlestickData.value.forEach { candlestick ->
            val jsonObject = JSONObject().apply {
                put("time", candlestick.time)
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