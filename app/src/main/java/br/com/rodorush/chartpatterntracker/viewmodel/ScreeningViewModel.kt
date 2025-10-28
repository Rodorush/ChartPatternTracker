package br.com.rodorush.chartpatterntracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.model.PatternItem
import br.com.rodorush.chartpatterntracker.model.ScreeningResult
import br.com.rodorush.chartpatterntracker.model.TimeframeItem
import br.com.rodorush.chartpatterntracker.viewmodel.ChartViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

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

    // Stores the state of the "All/None" switches for each selection screen
    private val _allPatternsChecked = MutableStateFlow(false)
    val allPatternsChecked: StateFlow<Boolean> = _allPatternsChecked

    private val _allAssetsChecked = MutableStateFlow(false)
    val allAssetsChecked: StateFlow<Boolean> = _allAssetsChecked

    private val _allTimeframesChecked = MutableStateFlow(false)
    val allTimeframesChecked: StateFlow<Boolean> = _allTimeframesChecked

    private var screeningJob: Job? = null

    // Cache simples de metadados do ativo (short/long name) por ticker
    private val assetInfoCache = mutableMapOf<String, Pair<String?, String?>>()
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://brapi.dev/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    private val brapiService by lazy { retrofit.create(BrapiApiService::class.java) }

    private suspend fun ensureAssetNamesCached(ticker: String) = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("assets").document(ticker)
            val doc = docRef.get().await()
            val hasShort = !doc.getString("shortName").isNullOrBlank()
            val hasLong = !doc.getString("longName").isNullOrBlank()
            if (hasShort && hasLong) return@withContext

            val resp = brapiService.getQuoteInfo(ticker, BuildConfig.BRAPI_TOKEN)
            val info = resp.results.firstOrNull()
            val sn = normalizeName(info?.shortName)
            val ln = normalizeName(info?.longName)
            val data = mutableMapOf<String, Any>()
            if (!hasShort && sn != null) data["shortName"] = sn
            if (!hasLong && ln != null) data["longName"] = ln
            if (data.isNotEmpty()) {
                docRef.set(data, SetOptions.merge()).await()
                Log.d("ScreeningViewModel", "Persistidos metadados no Firestore para $ticker: ${data.keys}")
            }
        } catch (e: Exception) {
            Log.w("ScreeningViewModel", "Falha ao garantir cache de nomes para $ticker: ${e.message}")
        }
    }

    private suspend fun fetchAssetInfo(ticker: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        assetInfoCache[ticker]?.let { return@withContext it }
        try {
            val resp = brapiService.getQuoteInfo(ticker, BuildConfig.BRAPI_TOKEN)
            val info = resp.results.firstOrNull()
            val sn = normalizeName(info?.shortName)
            val ln = normalizeName(info?.longName)
            val pair = Pair(sn, ln)
            // Persistir em Firestore se ainda não existir
            try {
                val db = FirebaseFirestore.getInstance()
                val docRef = db.collection("assets").document(ticker)
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
                // Ignora falhas de cache para não impactar o fluxo
            }
            assetInfoCache[ticker] = pair
            pair
        } catch (e: Exception) {
            Pair(null, null)
        }
    }

    private fun normalizeName(s: String?): String? {
        if (s.isNullOrBlank()) return null
        return s.trim().replace("\\s+".toRegex(), " ")
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

    fun setAllPatternsChecked(value: Boolean) {
        _allPatternsChecked.value = value
    }

    fun setAllAssetsChecked(value: Boolean) {
        _allAssetsChecked.value = value
    }

    fun setAllTimeframesChecked(value: Boolean) {
        _allTimeframesChecked.value = value
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
                                    // Enriquecer AssetItem com shortName/longName reais do /quote/{ticker}
                                    val (sn, ln) = fetchAssetInfo(asset.ticker)
                                    val enrichedAsset = asset.copy(
                                        shortName = sn ?: asset.shortName,
                                        longName = ln ?: asset.longName
                                    )
                                    results.add(
                                        ScreeningResult(
                                            pattern = pattern,
                                            asset = enrichedAsset,
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
