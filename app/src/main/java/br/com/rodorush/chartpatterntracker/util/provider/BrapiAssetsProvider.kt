package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.BuildConfig
import br.com.rodorush.chartpatterntracker.api.BrapiApiService
import br.com.rodorush.chartpatterntracker.api.BrapiApiService.StockQuote
import br.com.rodorush.chartpatterntracker.model.AssetItem
import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AssetsProvider
import br.com.rodorush.customlogs.Log
import kotlinx.coroutines.CancellationException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Objects

private const val TAG = "BrapiAssetsProvider"

/**
 * Lista de ativos do `/quote/list` da Brapi, com a última carga guardada em [cache] para quando
 * a Brapi não responde. Vive como `single` no Koin (`di/AppModule.kt`).
 */
class BrapiAssetsProvider internal constructor(
    private val cache: AssetListCache
) : AssetsProvider {
    private val token = BuildConfig.BRAPI_TOKEN
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://brapi.dev/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(BrapiApiService::class.java)

    override suspend fun fetchAssets(useLastLoad: Boolean, onResult: (List<AssetItem>) -> Unit) {
        val stocks = loadQuoteList(
            fromBrapi = { service.getQuoteList("Bearer $token").stocks },
            cache = cache,
            useLastLoad = useLastLoad
        )
        onResult(stocks.map { it.toAssetItem() })
    }
}

/**
 * Lista de ativos da Brapi, com a última carga como reserva.
 *
 * A lista da Brapi, quando chega com algum ativo, substitui a salva no [cache]. Se ela falha ou
 * chega vazia, a lista salva é devolvida quando [useLastLoad] permite. As duas fontes perdem as
 * entradas sem ticker ou com ticker em branco.
 *
 * @param useLastLoad `false` para a tela que mostra o preço como atual: sem a Brapi, a lista
 * volta vazia em vez de trazer os preços da última carga.
 * @return lista vazia quando nenhuma fonte tem ativos.
 */
internal suspend fun loadQuoteList(
    fromBrapi: suspend () -> List<StockQuote>,
    cache: AssetListCache,
    useLastLoad: Boolean
): List<StockQuote> {
    var failure: Exception? = null
    try {
        val stocks = fromBrapi().withTicker()
        if (stocks.isNotEmpty()) {
            Log.d(TAG, "Carregados ${stocks.size} ativos do /quote/list")
            saveQuietly(cache, stocks)
            return stocks
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failure = e
    }

    val saved = if (useLastLoad) cache.load().withTicker() else emptyList()
    when {
        // Com a exceção, a falha da Brapi segue chegando ao Crashlytics: sem ela, um token
        // revogado ficaria escondido enquanto houvesse lista salva.
        saved.isNotEmpty() && failure != null ->
            Log.w(TAG, "Brapi sem a lista de ativos; servida a última carga (${saved.size} ativos)", failure)
        saved.isNotEmpty() ->
            Log.w(TAG, "Brapi devolveu a lista de ativos vazia; servida a última carga (${saved.size} ativos)")
        failure != null -> Log.e(TAG, "Erro ao carregar lista de ativos", failure)
        else -> Log.w(TAG, "Brapi devolveu a lista de ativos vazia")
    }
    return saved
}

/** A lista nova vale para esta tela mesmo quando não pôde ser guardada para a próxima. */
private suspend fun saveQuietly(cache: AssetListCache, stocks: List<StockQuote>) {
    try {
        cache.save(stocks)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Lista de ativos não foi guardada para uso sem rede", e)
    }
}

/**
 * Descarta a entrada nula ou sem ticker. O Gson ignora a nulidade do Kotlin, e um `!= null` num
 * tipo não nulo pode ser removido pelo compilador; `Objects.nonNull` confere de fato.
 */
private fun List<StockQuote>.withTicker(): List<StockQuote> =
    filter { Objects.nonNull(it) && Objects.nonNull(it.stock) && it.stock.isNotBlank() }

private fun StockQuote.toAssetItem(): AssetItem {
    val norm = normalizeName(name)
    return AssetItem(
        ticker = stock,
        shortName = norm, // será substituído por shortName do Firestore no item visível, quando houver
        longName = norm,
        name = norm,
        lastPrice = close ?: 0.0,
        changePercent = change ?: 0.0,
        logo = logo ?: ""
    )
}

private fun normalizeName(s: String?): String {
    if (s.isNullOrBlank()) return ""
    return s.trim().replace("\\s+".toRegex(), " ")
}
