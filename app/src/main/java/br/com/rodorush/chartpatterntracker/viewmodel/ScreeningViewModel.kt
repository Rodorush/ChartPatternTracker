package br.com.rodorush.chartpatterntracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

    val isLoading: StateFlow<Boolean> = chartViewModel.isLoading

    fun updateSelectedPatterns(patterns: List<PatternItem>) {
        _selectedPatterns.value = patterns
    }

    fun updateSelectedAssets(assets: List<AssetItem>) {
        _selectedAssets.value = assets
    }

    fun updateSelectedTimeframes(timeframes: List<TimeframeItem>) {
        _selectedTimeframes.value = timeframes
    }

    fun startScreening() {
        Log.d("ScreeningViewModel", "Iniciando startScreening")
        Log.d("ScreeningViewModel", "Padrões selecionados: ${_selectedPatterns.value.map { it.id to it.name }}")
        Log.d("ScreeningViewModel", "Ativos selecionados: ${_selectedAssets.value.map { it.ticker }}")
        Log.d("ScreeningViewModel", "Timeframes selecionados: ${_selectedTimeframes.value.map { it.value }}")
        viewModelScope.launch {
            val results = mutableListOf<ScreeningResult>()
            val pattern = _selectedPatterns.value.firstOrNull { it.id == "48" }
            if (pattern == null) {
                Log.w("ScreeningViewModel", "Nenhum padrão Harami de Alta (ID 48) selecionado")
                return@launch
            }
            Log.d("ScreeningViewModel", "Padrão Harami de Alta selecionado: ${pattern.id} - ${pattern.name}")

            for (asset in _selectedAssets.value) {
                for (timeframe in _selectedTimeframes.value) {
                    try {
                        Log.d("ScreeningViewModel", "Chamando fetchData para ticker=${asset.ticker}, timeframe=${timeframe.value}")
                        chartViewModel.fetchData(asset.ticker, "3mo", timeframe.value)
                        val patternsDetected = chartViewModel.candlestickData.first { it.isNotEmpty() }
                        Log.d("ScreeningViewModel", "Detectados ${patternsDetected.size} padrões Harami de Alta para ${asset.ticker}-${timeframe.value}")
                        if (patternsDetected.isNotEmpty()) {
                            results.add(
                                ScreeningResult(
                                    pattern = pattern,
                                    asset = asset,
                                    timeframe = timeframe,
                                    reliability = "★★★",
                                    indication = "Reversão Alta",
                                    indicationIcon = br.com.rodorush.chartpatterntracker.R.drawable.ic_up_arrow
                                )
                            )
                            _screeningResults.value = results.toList()
                            Log.d("ScreeningViewModel", "Resultado adicionado para ${asset.ticker}-${timeframe.value}")
                        } else {
                            Log.d("ScreeningViewModel", "Nenhum padrão Harami de Alta detectado para ${asset.ticker}-${timeframe.value}")
                        }
                    } catch (e: Exception) {
                        Log.e("ScreeningViewModel", "Erro ao processar ${asset.ticker}-${timeframe.value}: ${e.message}", e)
                    }
                }
            }
            Log.d("ScreeningViewModel", "startScreening concluído com ${results.size} resultados")
        }
    }
}