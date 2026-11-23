package br.com.rodorush.chartpatterntracker.viewmodel

import androidx.lifecycle.ViewModel
import br.com.rodorush.customlogs.Log
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.IntervalNotInPlanException
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningPair
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.util.loadImageUrlFromStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

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

    private val _screeningFailures = MutableStateFlow<List<ScreeningPair>>(emptyList())
    val screeningFailures: StateFlow<List<ScreeningPair>> = _screeningFailures

    private val _dismissedFailures = MutableStateFlow<Set<ScreeningPair>>(emptySet())

    /**
     * Falhas de [screeningFailures] que o usuário já dispensou. O aviso só aparece enquanto
     * houver falha fora deste conjunto, então uma falha nova o traz de volta. Vale até a
     * seleção mudar: a varredura refeita depois de um cancelamento não reabre o aviso.
     */
    val dismissedFailures: StateFlow<Set<ScreeningPair>> = _dismissedFailures

    private val _stalePairs = MutableStateFlow<List<ScreeningPair>>(emptyList())

    /**
     * Pares varridos sobre a série vencida do Room, porque nenhuma fonte respondeu. Os achados
     * deles entram em [screeningResults] normalmente; a tela avisa que podem estar desatualizados.
     */
    val stalePairs: StateFlow<List<ScreeningPair>> = _stalePairs

    private val _dismissedStale = MutableStateFlow<Set<ScreeningPair>>(emptySet())

    /** Pares de [stalePairs] que o usuário já dispensou, com a mesma regra de [dismissedFailures]. */
    val dismissedStale: StateFlow<Set<ScreeningPair>> = _dismissedStale

    private val _shouldRefresh = MutableStateFlow(true)
    val shouldRefresh: StateFlow<Boolean> = _shouldRefresh

    private val _isScreening = MutableStateFlow(false)
    val isScreening: StateFlow<Boolean> = _isScreening

    private val _allPatternsChecked = MutableStateFlow(false)
    val allPatternsChecked: StateFlow<Boolean> = _allPatternsChecked

    private val _allAssetsChecked = MutableStateFlow(false)
    val allAssetsChecked: StateFlow<Boolean> = _allAssetsChecked

    private val _allTimeframesChecked = MutableStateFlow(false)
    val allTimeframesChecked: StateFlow<Boolean> = _allTimeframesChecked

    private var screeningJob: Job? = null

    private val patternImageCache = ConcurrentHashMap<String, String>()

    private suspend fun fetchPatternImage(patternId: String): String? {
        patternImageCache[patternId]?.let { return it }
        return loadImageUrlFromStorage(patternId)?.also { patternImageCache[patternId] = it }
    }

    // As três atualizações de seleção cancelam a varredura em curso: ela foi lançada com a
    // seleção anterior e, sem isso, startScreening a manteria no lugar da nova.
    fun updateSelectedPatterns(patterns: List<PatternItem>) {
        cancelScreening()
        _selectedPatterns.value = patterns
        onSelectionChanged()
    }

    fun updateSelectedAssets(assets: List<AssetItem>) {
        cancelScreening()
        _selectedAssets.value = assets
        onSelectionChanged()
    }

    fun updateSelectedTimeframes(timeframes: List<TimeframeItem>) {
        cancelScreening()
        _selectedTimeframes.value = timeframes
        onSelectionChanged()
    }

    private fun onSelectionChanged() {
        _shouldRefresh.value = true
        _dismissedFailures.value = emptySet()
        _dismissedStale.value = emptySet()
    }

    fun setAllPatternsChecked(value: Boolean) { _allPatternsChecked.value = value }
    fun setAllAssetsChecked(value: Boolean) { _allAssetsChecked.value = value }
    fun setAllTimeframesChecked(value: Boolean) { _allTimeframesChecked.value = value }

    /**
     * Varre cada par (ativo, tempo gráfico) uma única vez, com todos os padrões selecionados,
     * e publica os achados em [screeningResults] à medida que saem. O par cuja série não pôde
     * ser obtida, por erro, por recusa do plano do token da Brapi ou por tempo esgotado, vai
     * para [screeningFailures]. Só o erro vai para o Crashlytics. O par varrido sobre a série
     * vencida do Room, porque nenhuma fonte respondeu ou o tempo esgotou, vai para [stalePairs]
     * e só cai nas falhas quando o Room também não tem a série. Se for
     * cancelada, [shouldRefresh] continua ligado e a tela a refaz ao reabrir.
     *
     * Com uma varredura em curso, a chamada não faz nada: é o que preserva os resultados
     * acumulados quando a tela volta do gráfico. Mudar a seleção cancela a varredura
     * em curso, então a que está rodando é sempre a da seleção atual.
     */
    fun startScreening() {
        if (screeningJob?.isActive == true) return
        screeningJob = viewModelScope.launch {
            _isScreening.value = true
            _screeningResults.value = emptyList()
            _screeningFailures.value = emptyList()
            _stalePairs.value = emptyList()
            val results = mutableListOf<ScreeningResult>()
            val patterns = _selectedPatterns.value
            val patternIds = patterns.map { it.id }
            try {
                for (asset in _selectedAssets.value) {
                    for (timeframe in _selectedTimeframes.value) {
                        val scan = try {
                            withTimeoutOrNull(SCREENING_TIMEOUT_MS) {
                                chartViewModel.runScreening(
                                    ticker = asset.ticker,
                                    range = SCREENING_RANGE,
                                    interval = timeframe.value,
                                    patternIds = patternIds
                                )
                            } ?: run {
                                // Rede lenta não chega a falhar: o tempo limite corta a busca
                                // antes do Room vencido, que vira o recurso aqui.
                                Log.e(TAG, "Timeout em ${asset.ticker}-${timeframe.value}")
                                chartViewModel.runStaleScreening(
                                    ticker = asset.ticker,
                                    interval = timeframe.value,
                                    patternIds = patternIds
                                )
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: IntervalNotInPlanException) {
                            // Recusa esperada, já registrada no repositório: o par vai para
                            // as falhas sem virar erro no Crashlytics.
                            null
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Erro durante screening",
                                e,
                                "ticker" to asset.ticker,
                                "timeframe" to timeframe.value
                            )
                            null
                        }
                        if (scan == null) {
                            _screeningFailures.update { it + ScreeningPair(asset.ticker, timeframe) }
                            continue
                        }
                        if (scan.stale) {
                            _stalePairs.update { it + ScreeningPair(asset.ticker, timeframe) }
                        }
                        val found = patterns.filter { !scan.patterns[it.id].isNullOrEmpty() }
                        if (found.isEmpty()) continue

                        for (pattern in found) {
                            val patternImage = fetchPatternImage(pattern.id)

                            val reliabilityText = pattern.getLocalized("reliability")
                            val indicationText = pattern.getLocalized("indication")
                            results.add(
                                ScreeningResult(
                                    pattern = pattern,
                                    asset = asset,
                                    timeframe = timeframe,
                                    reliability = convertReliabilityToStars(reliabilityText),
                                    indication = indicationText,
                                    indicationIcon = convertIndicationToIcon(indicationText),
                                    patternImageUrl = patternImage
                                )
                            )
                            _screeningResults.value = results.toList()
                        }
                    }
                }
                // Só a varredura que chega ao fim dispensa a próxima; a cancelada é refeita.
                _shouldRefresh.value = false
            } finally {
                _isScreening.value = false
            }
        }
    }

    /** Fecha o aviso de falhas até surgir uma falha nova ou a seleção mudar. */
    fun dismissFailures() {
        _dismissedFailures.value = _screeningFailures.value.toSet()
    }

    /** Fecha o aviso de séries vencidas até surgir um par novo ou a seleção mudar. */
    fun dismissStale() {
        _dismissedStale.value = _stalePairs.value.toSet()
    }

    /**
     * Interrompe a varredura em curso. Chamado ao sair da tela de resultados por Voltar ou
     * Home e ao mudar a seleção; abrir o gráfico de um resultado não cancela.
     */
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
            "reversão baixista", "bearish reversal", "reversión bajista" -> R.drawable.reversao_baixista
            "reversão altista", "bullish reversal", "reversión alcista" -> R.drawable.reversao_altista
            "continuação de baixa", "bearish continuation", "continuación bajista" -> R.drawable.continuacao_baixa
            "continuação de alta", "bullish continuation", "continuación alcista" -> R.drawable.continuacao_alta
            else -> R.drawable.ic_up_arrow
        }
    }

    private companion object {
        const val TAG = "ScreeningViewModel"
        const val SCREENING_TIMEOUT_MS = 15_000L
        const val SCREENING_RANGE = "3mo"
    }
}
