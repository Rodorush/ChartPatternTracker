package br.com.rodorush.chartpatterntracker.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.data.AlphaVantageDataSource
import br.com.rodorush.chartpatterntracker.data.AssetDataSource
import br.com.rodorush.chartpatterntracker.data.BrapiDataSource
import br.com.rodorush.chartpatterntracker.data.MockDataSource
import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.CandlestickRepository
import br.com.rodorush.chartpatterntracker.model.PatternDisplayMode
import br.com.rodorush.chartpatterntracker.model.PatternOccurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ChartViewModel(
    private val preferences: SharedPreferences,
    private val repository: CandlestickRepository
) : ViewModel() {
    companion object {
        private const val PREF_PATTERN_DISPLAY_MODE = "pattern_display_mode"
    }

    private val _candlestickData = MutableStateFlow<List<Candlestick>>(emptyList())
    val candlestickData: StateFlow<List<Candlestick>> = _candlestickData

    private val _patternsData = MutableStateFlow<Map<String, List<PatternOccurrence>>>(emptyMap())
    val patternsData: StateFlow<Map<String, List<PatternOccurrence>>> = _patternsData

    private val _patternDisplayMode = MutableStateFlow(
        runCatching {
            PatternDisplayMode.valueOf(
                preferences.getString(PREF_PATTERN_DISPLAY_MODE, PatternDisplayMode.ALL.name)
                    ?: PatternDisplayMode.ALL.name
            )
        }.getOrDefault(PatternDisplayMode.ALL)
    )
    val patternDisplayMode: StateFlow<PatternDisplayMode> = _patternDisplayMode

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

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == PREF_PATTERN_DISPLAY_MODE) {
                val stored = sharedPrefs.getString(PREF_PATTERN_DISPLAY_MODE, PatternDisplayMode.ALL.name)
                    ?: PatternDisplayMode.ALL.name
                val newMode = runCatching { PatternDisplayMode.valueOf(stored) }
                    .getOrDefault(PatternDisplayMode.ALL)
                if (newMode != _patternDisplayMode.value) {
                    Log.d("ChartViewModel", "pattern_display_mode alterado via SharedPreferences para $newMode")
                    _patternDisplayMode.value = newMode
                    _patternsData.value = applyDisplayMode(lastDetectedPatterns, _candlestickData.value)
                }
            }
        }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        val savedSource = preferences.getString("data_source", "Brapi") ?: "Brapi"
        setDataSource(savedSource)
        if (BuildConfig.BRAPI_TOKEN.isBlank()) {
            Log.w("ChartViewModel", "BRAPI_TOKEN está vazio. Verifique BuildConfig.")
            _error.value = "Chave da API inválida"
        } else {
            _currentSource.value.setApiKey(BuildConfig.BRAPI_TOKEN)
        }
    }

    fun setPatternDisplayMode(mode: PatternDisplayMode) {
        if (_patternDisplayMode.value == mode) return
        _patternDisplayMode.value = mode
        preferences.edit { putString(PREF_PATTERN_DISPLAY_MODE, mode.name) }
        _patternsData.value = applyDisplayMode(lastDetectedPatterns, _candlestickData.value)
    }

    private fun applyDisplayMode(
        detected: Map<String, List<PatternOccurrence>>,
        candles: List<Candlestick>
    ): Map<String, List<PatternOccurrence>> {
        return when (patternDisplayMode.value) {
            PatternDisplayMode.ALL -> detected
            PatternDisplayMode.ONLY_RECENT -> {
                if (candles.isEmpty()) return emptyMap()
                detected.mapValues { entry ->
                    entry.value.filter { occ ->
                        val k = occ.candles.size
                        if (candles.size < k) return@filter false
                        val tail = candles.takeLast(k)
                        occ.candles == tail
                    }
                }.filterValues { it.isNotEmpty() }
            }
        }
    }

    private var lastDetectedPatterns: Map<String, List<PatternOccurrence>> = emptyMap()

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

    fun fetchData(
        ticker: String,
        range: String,
        interval: String,
        detectPatterns: Boolean = true,
        patternIds: List<String> = emptyList()
    ) {
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
                var finalCandlesticks: List<Candlestick>? = null

                runCatching { repository.fetchCandlesticks(ticker, interval, range) }
                    .onSuccess { cached ->
                        if (cached.isNotEmpty()) {
                            finalCandlesticks = cached
                            Log.d(
                                "ChartViewModel",
                                "Candlesticks obtidos via Room/Firestore: ${cached.size}"
                            )
                        } else {
                            Log.d(
                                "ChartViewModel",
                                "Cache local vazio para $ticker-$interval; tentando fonte remota"
                            )
                        }
                    }
                    .onFailure { e ->
                        Log.w(
                            "ChartViewModel",
                            "Falha ao consultar Room/Firestore para $ticker-$interval: ${e.message}",
                            e
                        )
                    }

                if (finalCandlesticks == null) {
                    Log.d(
                        "ChartViewModel",
                        "Fonte de dados atual: ${_currentSource.value.javaClass.simpleName}"
                    )
                    val remoteCandlesticks =
                        _currentSource.value.getHistoricalData(ticker, range, interval)
                    remoteCandlesticks.onSuccess { remoteData ->
                        Log.d(
                            "ChartViewModel",
                            "Fonte remota retornou ${remoteData.size} candlesticks"
                        )
                        if (remoteData.isNotEmpty()) {
                            finalCandlesticks = remoteData
                        } else {
                            Log.w(
                                "ChartViewModel",
                                "Nenhum dado remoto retornado para $ticker"
                            )
                            _error.value = "Nenhum dado disponível"
                        }
                    }.onFailure { e ->
                        Log.e(
                            "ChartViewModel",
                            "Erro ao obter dados remotos: ${e.message}",
                            e
                        )
                        _error.value = e.message ?: "Falha ao buscar dados iniciais"
                    }
                }

                val candlesticks = finalCandlesticks.orEmpty()
                if (candlesticks.isEmpty()) {
                    _candlestickData.value = emptyList()
                    _patternsData.value = emptyMap()
                } else {
                    _candlestickData.value = candlesticks
                    if (detectPatterns) {
                        val detected = repository.detectPatterns(patternIds, candlesticks)
                        lastDetectedPatterns = detected
                        _patternsData.value = applyDisplayMode(detected, candlesticks)
                        Log.d(
                            "ChartViewModel",
                            "Padrões detectados: " + detected.map { it.key to it.value.size }
                        )
                    } else {
                        _patternsData.value = emptyMap()
                    }
                }
            } catch (e: Exception) {
                Log.e("ChartViewModel", "Erro inesperado em fetchData: ${e.message}", e)
                _candlestickData.value = emptyList()
                _patternsData.value = emptyMap()
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

    override fun onCleared() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        super.onCleared()
    }
}