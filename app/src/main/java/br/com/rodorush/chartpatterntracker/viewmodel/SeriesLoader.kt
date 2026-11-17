package br.com.rodorush.chartpatterntracker.viewmodel

import br.com.rodorush.chartpatterntracker.model.Candlestick
import br.com.rodorush.chartpatterntracker.model.IntervalNotInPlanException
import kotlinx.coroutines.CancellationException

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
 * A recusa do plano do token da Brapi encerra a busca, porque a Brapi direta recusaria o
 * mesmo intervalo e o Room não tem a série. A falha de uma fonte passa para a seguinte.
 *
 * @param fromRepository série válida do repositório; vazia ou com exceção passa adiante.
 * @param fromBrapi série da Brapi direta; vazia ou com falha passa para o Room vencido.
 * @param fromStaleCache série do Room sem conferir a validade.
 * @return série vazia, com `stale = false`, quando nenhuma fonte falhou e nenhuma tem velas.
 * @throws IntervalNotInPlanException quando o repositório recusa o par pelo plano.
 * @throws Exception a falha da Brapi direta, quando o Room também não tem a série.
 */
internal suspend fun loadSeries(
    fromRepository: suspend () -> List<Candlestick>,
    fromBrapi: suspend () -> Result<List<Candlestick>>,
    fromStaleCache: suspend () -> List<Candlestick>
): SeriesLoad {
    val cached = try {
        fromRepository()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IntervalNotInPlanException) {
        throw e
    } catch (e: Exception) {
        // O repositório já registrou o erro; a Brapi direta é a próxima tentativa.
        emptyList()
    }
    if (cached.isNotEmpty()) return SeriesLoad(cached, stale = false)

    val direct = fromBrapi()
    val directFailure = direct.exceptionOrNull()
    // A fonte direta captura Exception e devolve o cancelamento dentro do Result.
    if (directFailure is CancellationException) throw directFailure
    val directCandles = direct.getOrNull().orEmpty()
    if (directCandles.isNotEmpty()) return SeriesLoad(directCandles, stale = false)

    val stale = fromStaleCache()
    if (stale.isNotEmpty()) return SeriesLoad(stale, stale = true)
    if (directFailure != null) throw directFailure
    return SeriesLoad(emptyList(), stale = false)
}
