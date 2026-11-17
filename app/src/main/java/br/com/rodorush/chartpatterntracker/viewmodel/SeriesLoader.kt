package br.com.rodorush.chartpatterntracker.viewmodel

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.IntervalNotInPlanException
import br.com.rodorush.customlogs.Log
import kotlinx.coroutines.CancellationException

private const val TAG = "SeriesLoader"

/**
 * Série de um par e a origem dela.
 *
 * @property stale `true` quando a série veio do Room sem valer mais, porque nenhuma fonte
 * respondeu: quem a mostra avisa que ela pode estar desatualizada.
 */
data class SeriesLoad(
    val candles: List<Candlestick>,
    val stale: Boolean
)

/**
 * Busca a série de um par nas fontes em ordem: o repositório (Room válido, Firestore e a
 * `updateCandlesticks`), a Brapi direta e, por último, o Room vencido.
 *
 * O Room vencido só é lido quando o repositório falhou: se ele respondeu vazio, o Room não tem
 * a série. A recusa do plano do token da Brapi encerra a busca, porque a Brapi direta recusaria
 * o mesmo intervalo e o Room não tem a série.
 *
 * @param ticker e [timeframe] identificam o par no log.
 * @param fromRepository série válida do repositório; vazia ou com exceção passa adiante.
 * @param fromBrapi série da Brapi direta; vazia ou com falha passa para o Room vencido.
 * @param fromStaleCache série do Room sem conferir a validade.
 * @return série vazia, com `stale = false`, quando nenhuma fonte tem velas e a Brapi direta
 * respondeu sem falha, mesmo que o repositório tenha falhado.
 * @throws IntervalNotInPlanException quando o repositório recusa o par pelo plano.
 * @throws Exception a falha da Brapi direta, quando o Room também não tem a série.
 */
internal suspend fun loadSeries(
    ticker: String,
    timeframe: String,
    fromRepository: suspend () -> List<Candlestick>,
    fromBrapi: suspend () -> Result<List<Candlestick>>,
    fromStaleCache: suspend () -> List<Candlestick>
): SeriesLoad {
    var repositoryFailed = false
    val cached = try {
        fromRepository()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IntervalNotInPlanException) {
        throw e
    } catch (e: Exception) {
        // O repositório já registrou o erro; a Brapi direta é a próxima tentativa.
        repositoryFailed = true
        emptyList()
    }
    if (cached.isNotEmpty()) return SeriesLoad(cached, stale = false)

    val direct = fromBrapi()
    val directFailure = direct.exceptionOrNull()
    // A fonte direta captura Exception e devolve o cancelamento dentro do Result.
    if (directFailure is CancellationException) throw directFailure
    val directCandles = direct.getOrNull().orEmpty()
    if (directCandles.isNotEmpty()) return SeriesLoad(directCandles, stale = false)

    val stale = if (repositoryFailed) readStaleCache(ticker, timeframe, fromStaleCache) else emptyList()
    if (stale.isNotEmpty()) {
        val message = "Nenhuma fonte respondeu; série vencida do Room para $ticker-$timeframe"
        if (directFailure != null) {
            // Com a exceção, a falha da Brapi direta segue chegando ao Crashlytics: sem ela, um
            // token revogado ficaria escondido enquanto houvesse série no Room.
            Log.w(TAG, message, directFailure, "ticker" to ticker, "timeframe" to timeframe)
        } else {
            Log.w(TAG, message)
        }
        return SeriesLoad(stale, stale = true)
    }
    if (directFailure != null) throw directFailure
    return SeriesLoad(emptyList(), stale = false)
}

/** Lê o Room vencido; a falha dele não pode esconder a da Brapi direta, que é a relançada. */
private suspend fun readStaleCache(
    ticker: String,
    timeframe: String,
    fromStaleCache: suspend () -> List<Candlestick>
): List<Candlestick> = try {
    fromStaleCache()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(TAG, "Falha ao ler o Room vencido de $ticker-$timeframe: ${e.message}")
    emptyList()
}
