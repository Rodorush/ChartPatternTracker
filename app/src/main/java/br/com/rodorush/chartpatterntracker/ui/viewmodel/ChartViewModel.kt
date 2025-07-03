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
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
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

    private val _patternsData = MutableStateFlow<List<PatternOccurrence>>(emptyList())
    val patternsData: StateFlow<List<PatternOccurrence>> = _patternsData

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
        if (BuildConfig.BRAPI_TOKEN.isBlank()) {
            Log.w("ChartViewModel", "BRAPI_TOKEN está vazio. Verifique BuildConfig.")
            _error.value = "Chave da API inválida"
        } else {
            _currentSource.value.setApiKey(BuildConfig.BRAPI_TOKEN)
        }
    }

    fun setDataSource(sourceName: String) {
        val newSource = sources[sourceName] ?: return
        _currentSource.value = newSource
        preferences.edit { putString("data_source", sourceName) }
        Log.d("ChartViewModel", "Fonte de dados alterada para: $sourceName")
    }

    fun setApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            Log.w("ChartViewModel", "Tentativa de definir API key vazia")
            _error.value = "Chave da API não pode ser vazia"
            return
        }
        _currentSource.value.setApiKey(apiKey)
        Log.d("ChartViewModel", "API key definida com sucesso")
    }

    fun fetchData(ticker: String, range: String, interval: String, detectPatterns: Boolean = true) {
        if (ticker.isBlank() || interval.isBlank()) {
            Log.e("ChartViewModel", "Ticker ou interval inválidos: ticker=$ticker, interval=$interval")
            _error.value = "Parâmetros inválidos"
            _isLoading.value = false
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            Log.d("ChartViewModel", "Iniciando fetchData para ticker=$ticker, range=$range, interval=$interval")
            try {
                Log.d("ChartViewModel", "Fonte de dados atual: ${_currentSource.value.javaClass.simpleName}")
                val initialCandlesticks = _currentSource.value.getHistoricalData(ticker, range, interval)
                initialCandlesticks.onSuccess { initialData ->
                    Log.d("ChartViewModel", "Dados iniciais obtidos: ${initialData.size} candlesticks")
                    if (initialData.isEmpty()) {
                        Log.w("ChartViewModel", "Nenhum dado inicial retornado para $ticker")
                        _error.value = "Nenhum dado disponível"
                        _candlestickData.value = emptyList()
                    } else {
                        val fullCandlesticks = repository.fetchCandlesticks(ticker, interval, range)
                        Log.d("ChartViewModel", "Candlesticks completos obtidos: ${fullCandlesticks.size}")
                        if (fullCandlesticks.isNotEmpty()) {
                            _candlestickData.value = fullCandlesticks
                            if (detectPatterns) {
                                val haramiPatterns = repository.detectHaramiAlta(fullCandlesticks)
                                Log.d(
                                    "ChartViewModel",
                                    "Padrões Harami de Alta detectados: ${haramiPatterns.size}"
                                )
                                _patternsData.value = haramiPatterns
                            } else {
                                _patternsData.value = emptyList()
                            }
                        } else {
                            Log.w("ChartViewModel", "Nenhum candlestick completo retornado para $ticker")
                            _candlestickData.value = emptyList()
                            _patternsData.value = emptyList()
                            _error.value = "Falha ao obter dados completos"
                        }
                    }
                }.onFailure { e ->
                    Log.e("ChartViewModel", "Erro ao obter dados iniciais: ${e.message}", e)
                    _candlestickData.value = emptyList()
                    _patternsData.value = emptyList()
                    _error.value = e.message ?: "Falha ao buscar dados iniciais"
                }
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Erro inesperado em fetchData: ${e.message}", e)
                _candlestickData.value = emptyList()
                _error.value = e.message ?: "Erro inesperado"
            } finally {
                _isLoading.value = false
                Log.d("ChartViewModel", "fetchData concluído")
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