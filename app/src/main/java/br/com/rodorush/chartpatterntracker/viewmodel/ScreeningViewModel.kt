package br.com.rodorush.chartpatterntracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ScreeningViewModel(
    private val chartViewModel: ChartViewModel
) : ViewModel() {
    private val _selectedPatterns = MutableStateFlow<List<PatternItem>>(emptyList())
    val selectedPatterns: StateFlow<List<PatternItem>> = _selectedPatterns

    private val _selectedAssets = MutableStateFlow<List<AssetItem>>(emptyList())
    val selectedAssets: StateFlow<List<AssetItem>> = _selectedAssets

    private val _selectedTimeframes = MutableStateFlow<List<TimeframeItem>>(emptyList())
    val selectedTimeframes: StateFlow<List<TimeframeItem>> = _selectedTimeframes

    private val _screeningResults = MutableStateFlow<List<ScreeningResult>>(emptyList())
    val screeningResults: StateFlow<List<ScreeningResult>> = _screeningResults

    // Flag to indicate if a new screening run is required
    private val _shouldRefresh = MutableStateFlow(true)
    val shouldRefresh: StateFlow<Boolean> = _shouldRefresh

    private val _isScreening = MutableStateFlow(false)
    val isScreening: StateFlow<Boolean> = _isScreening

    private var screeningJob: Job? = null

    fun updateSelectedPatterns(patterns: List<PatternItem>) {
        _selectedPatterns.value = patterns
        _shouldRefresh.value = true
    }

    fun updateSelectedAssets(assets: List<AssetItem>) {
        _selectedAssets.value = assets
        _shouldRefresh.value = true
    }

    fun updateSelectedTimeframes(timeframes: List<TimeframeItem>) {
        _selectedTimeframes.value = timeframes
        _shouldRefresh.value = true
    }

    fun startScreening() {
        Log.d("ScreeningViewModel", "Iniciando startScreening")
        Log.d("ScreeningViewModel", "Padrões selecionados: ${_selectedPatterns.value.map { it.id to it.name }}")
        Log.d("ScreeningViewModel", "Ativos selecionados: ${_selectedAssets.value.map { it.ticker }}")
        Log.d("ScreeningViewModel", "Timeframes selecionados: ${_selectedTimeframes.value.map { it.value }}")
        screeningJob?.cancel()
        screeningJob = viewModelScope.launch {
            _isScreening.value = true
            _screeningResults.value = emptyList()
            val results = mutableListOf<ScreeningResult>()
            try {
                for (pattern in _selectedPatterns.value) {
                    Log.d("ScreeningViewModel", "Processando padrão ${pattern.id}")
                    for (asset in _selectedAssets.value) {
                        for (timeframe in _selectedTimeframes.value) {
                            try {
                                Log.d("ScreeningViewModel", "Chamando fetchData para ticker=${asset.ticker}, timeframe=${timeframe.value}")
                                val patternsDetected = withTimeoutOrNull(15000L) {
                                    chartViewModel.fetchData(asset.ticker, "3mo", timeframe.value, true, listOf(pattern.id))
                                    chartViewModel.isLoading.first { !it }
                                    chartViewModel.patternsData.first()
                                }
                                if (patternsDetected == null) {
                                    Log.e("ScreeningViewModel", "Timeout ao processar ${asset.ticker}-${timeframe.value}")
                                    continue
                                }
                                if (chartViewModel.error.value != null) {
                                    Log.e(
                                        "ScreeningViewModel",
                                        "Erro retornado pelo ChartViewModel para ${asset.ticker}-${timeframe.value}: ${chartViewModel.error.value}"
                                    )
                                    continue
                                }
                                val occurrences = patternsDetected[pattern.id].orEmpty()
                                Log.d(
                                    "ScreeningViewModel",
                                    "Detectados ${occurrences.size} padrões ${pattern.id} para ${asset.ticker}-${timeframe.value}"
                                )
                                if (occurrences.isNotEmpty()) {
                                    val reliabilityText = pattern.getLocalized("reliability")
                                    val reliabilityStars = convertReliabilityToStars(reliabilityText)
                                    val indicationText = pattern.getLocalized("indication")
                                    val indicationIconRes = convertIndicationToIcon(indicationText)
                                    results.add(
                                        ScreeningResult(
                                            pattern = pattern,
                                            asset = asset,
                                            timeframe = timeframe,
                                            reliability = reliabilityStars,
                                            indication = indicationText,
                                            indicationIcon = indicationIconRes
                                        )
                                    )
                                    _screeningResults.value = results.toList()
                                }
                            } catch (e: Exception) {
                                Log.e("ScreeningViewModel", "Erro ao processar ${asset.ticker}-${timeframe.value}: ${e.message}", e)
                            }
                        }
                    }
                }
                Log.d("ScreeningViewModel", "startScreening concluído com ${results.size} resultados")
            } finally {
                _shouldRefresh.value = false
                _isScreening.value = false
            }
        }
    }

    fun cancelScreening() {
        screeningJob?.cancel()
        _isScreening.value = false
    }

    private fun convertReliabilityToStars(reliability: String): String {
        return when (reliability.lowercase()) {
            "baixa", "low", "baja" -> "★"
            "média", "media", "medium" -> "★★"
            "alta", "high" -> "★★★"
            else -> reliability
        }
    }

    private fun convertIndicationToIcon(indication: String): Int {
        return when (indication.lowercase()) {
            "reversão baixista", "bearish reversal", "reversión bajista" ->
                br.com.rodorush.chartpatterntracker.R.drawable.reversao_baixista
            "reversão altista", "bullish reversal", "reversión alcista" ->
                br.com.rodorush.chartpatterntracker.R.drawable.reversao_altista
            "continuação de baixa", "bearish continuation", "continuación bajista" ->
                br.com.rodorush.chartpatterntracker.R.drawable.continuacao_baixa
            "continuação de alta", "bullish continuation", "continuación alcista" ->
                br.com.rodorush.chartpatterntracker.R.drawable.continuacao_alta
            else -> br.com.rodorush.chartpatterntracker.R.drawable.ic_up_arrow
        }
    }
}
