package br.com.rodorush.chartpatterntracker.viewmodel

import androidx.lifecycle.ViewModel
import br.com.rodorush.customlogs.Log
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.R
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningFailure
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.util.loadImageUrlFromStorage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    private val _screeningFailures = MutableStateFlow<List<ScreeningFailure>>(emptyList())
    val screeningFailures: StateFlow<List<ScreeningFailure>> = _screeningFailures

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

    private val assetInfoCache = ConcurrentHashMap<String, Pair<String?, String?>>()
    private val patternImageCache = ConcurrentHashMap<String, String>()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://brapi.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val brapiService by lazy { retrofit.create(BrapiApiService::class.java) }

    private suspend fun fetchAssetInfo(ticker: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        assetInfoCache[ticker]?.let { return@withContext it }
        try {
            val resp = brapiService.getQuoteInfo(ticker, BuildConfig.BRAPI_TOKEN)
            val info = resp.results.firstOrNull()
            val sn = normalizeName(info?.shortName)
            val ln = normalizeName(info?.longName)
            val pair = sn to ln
            try {
                val docRef = FirebaseFirestore.getInstance().collection("assets").document(ticker)
                val doc = docRef.get().await()
                val existingSn = doc.getString("shortName")
                val existingLn = doc.getString("longName")
                if ((sn != null && existingSn.isNullOrBlank()) || (ln != null && existingLn.isNullOrBlank())) {
                    val data = mutableMapOf<String, Any>()
                    if (sn != null) data["shortName"] = sn
                    if (ln != null) data["longName"] = ln
                    if (data.isNotEmpty()) docRef.set(data, SetOptions.merge()).await()
                }
            } catch (_: Exception) {
                // Falha de cache no Firestore não impacta o resultado do screening.
            }
            assetInfoCache[ticker] = pair
            pair
        } catch (e: Exception) {
            Log.w(TAG, "Falha ao obter info do ativo $ticker: ${e.message}")
            null to null
        }
    }

    private suspend fun fetchPatternImage(patternId: String): String? {
        patternImageCache[patternId]?.let { return it }
        return loadImageUrlFromStorage(patternId)?.also { patternImageCache[patternId] = it }
    }

    private fun normalizeName(s: String?): String? {
        if (s.isNullOrBlank()) return null
        return s.trim().replace(WHITESPACE, " ")
    }

    private fun resolveDisplayShortName(asset: AssetItem, fetched: String?): String {
        val candidate = fetched
            ?: asset.shortName.takeIf { it.isNotBlank() }
            ?: asset.name.takeIf { it.isNotBlank() }
            ?: asset.longName.takeIf { it.isNotBlank() }
            ?: ""
        return candidate.trim().replace(WHITESPACE, " ")
    }

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

    fun setAllPatternsChecked(value: Boolean) { _allPatternsChecked.value = value }
    fun setAllAssetsChecked(value: Boolean) { _allAssetsChecked.value = value }
    fun setAllTimeframesChecked(value: Boolean) { _allTimeframesChecked.value = value }

    /**
     * Varre cada par (ativo, tempo gráfico) uma única vez, com todos os padrões selecionados,
     * e publica os achados em [screeningResults] à medida que saem. O par cuja série não pôde
     * ser obtida, por erro ou por tempo esgotado, vai para [screeningFailures].
     */
    fun startScreening() {
        screeningJob?.cancel()
        screeningJob = viewModelScope.launch {
            _isScreening.value = true
            _screeningResults.value = emptyList()
            _screeningFailures.value = emptyList()
            val results = mutableListOf<ScreeningResult>()
            val failures = mutableListOf<ScreeningFailure>()
            val patterns = _selectedPatterns.value
            val patternIds = patterns.map { it.id }
            try {
                for (asset in _selectedAssets.value) {
                    for (timeframe in _selectedTimeframes.value) {
                        try {
                            val detected = withTimeoutOrNull(SCREENING_TIMEOUT_MS) {
                                chartViewModel.runScreening(
                                    ticker = asset.ticker,
                                    range = SCREENING_RANGE,
                                    interval = timeframe.value,
                                    patternIds = patternIds
                                )
                            }
                            if (detected == null) {
                                Log.e(TAG, "Timeout em ${asset.ticker}-${timeframe.value}")
                                failures.add(ScreeningFailure(asset.ticker, timeframe))
                                _screeningFailures.value = failures.toList()
                                continue
                            }
                            for (pattern in patterns) {
                                if (detected[pattern.id].isNullOrEmpty()) continue

                                val (sn, ln) = fetchAssetInfo(asset.ticker)
                                val enrichedAsset = asset.copy(
                                    shortName = sn ?: asset.shortName,
                                    longName = ln ?: asset.longName
                                )
                                val displayShortName = resolveDisplayShortName(enrichedAsset, sn)
                                val patternImage = fetchPatternImage(pattern.id)

                                val reliabilityText = pattern.getLocalized("reliability")
                                val indicationText = pattern.getLocalized("indication")
                                results.add(
                                    ScreeningResult(
                                        pattern = pattern,
                                        asset = enrichedAsset,
                                        timeframe = timeframe,
                                        reliability = convertReliabilityToStars(reliabilityText),
                                        indication = indicationText,
                                        indicationIcon = convertIndicationToIcon(indicationText),
                                        displayShortName = displayShortName,
                                        patternImageUrl = patternImage
                                    )
                                )
                                _screeningResults.value = results.toList()
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Erro durante screening",
                                e,
                                "ticker" to asset.ticker,
                                "timeframe" to timeframe.value
                            )
                            failures.add(ScreeningFailure(asset.ticker, timeframe))
                            _screeningFailures.value = failures.toList()
                        }
                    }
                }
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
        val WHITESPACE = "\\s+".toRegex()
    }
}
