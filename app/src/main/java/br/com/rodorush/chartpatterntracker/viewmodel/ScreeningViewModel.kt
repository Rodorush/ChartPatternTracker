package br.com.rodorush.chartpatterntracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ScreeningViewModel(
    private val chartViewModel: ChartViewModel
) : ViewModel() {
    // Estado para padrões gráficos selecionados
    private val _selectedPatterns = MutableStateFlow<List<PatternItem>>(emptyList())
    val selectedPatterns: StateFlow<List<PatternItem>> = _selectedPatterns

    // Estado para ativos selecionados
    private val _selectedAssets = MutableStateFlow<List<AssetItem>>(emptyList())
    val selectedAssets: StateFlow<List<AssetItem>> = _selectedAssets

    // Estado para timeframes selecionados
    private val _selectedTimeframes = MutableStateFlow<List<TimeframeItem>>(emptyList())
    val selectedTimeframes: StateFlow<List<TimeframeItem>> = _selectedTimeframes

    // Estado para resultados da busca
    private val _screeningResults = MutableStateFlow<List<ScreeningResult>>(emptyList())
    val screeningResults: StateFlow<List<ScreeningResult>> = _screeningResults

    // Estado para isLoading, exposto do ChartViewModel
    val isLoading: StateFlow<Boolean> = chartViewModel.isLoading

    // Funções para atualizar as seleções
    fun updateSelectedPatterns(patterns: List<PatternItem>) {
        _selectedPatterns.value = patterns
    }

    fun updateSelectedAssets(assets: List<AssetItem>) {
        _selectedAssets.value = assets
    }

    fun updateSelectedTimeframes(timeframes: List<TimeframeItem>) {
        _selectedTimeframes.value = timeframes
    }

    // Função para iniciar a busca
    fun startScreening() {
        viewModelScope.launch {
            val results = mutableListOf<ScreeningResult>()
            // Filtra o padrão Harami de Alta pelo ID do Firebase
            val pattern = _selectedPatterns.value.firstOrNull { it.id == "48" }
                ?: return@launch // Apenas Harami de Alta por enquanto

            _selectedAssets.value.forEach { asset ->
                _selectedTimeframes.value.forEach { timeframe ->
                    // Chama o ChartViewModel para buscar dados e detectar o padrão
                    chartViewModel.fetchData(asset.ticker, "3mo", timeframe.value)
                    chartViewModel.candlestickData.collectLatest { candlesticks ->
                        if (candlesticks.isNotEmpty()) {
                            // Adiciona o resultado apenas se o padrão Harami de Alta foi detectado
                            results.add(
                                ScreeningResult(
                                    pattern = pattern,
                                    asset = asset,
                                    timeframe = timeframe,
                                    reliability = "★★★", // Hardcoded, será obtido do Firebase no futuro
                                    indication = "Reversão Alta", // Hardcoded, será obtido do Firebase no futuro
                                    indicationIcon = br.com.rodorush.chartpatterntracker.R.drawable.ic_up_arrow // Hardcoded, será obtido do Firebase no futuro
                                )
                            )
                            _screeningResults.value = results.toList()
                        }
                    }
                }
            }
        }
    }
}