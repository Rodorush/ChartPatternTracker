package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.api.BrapiApiService.QuoteListResponse
import br.com.rodorush.chartpatterntracker.api.BrapiApiService.StockQuote
import br.com.rodorush.customlogs.Log
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Última lista do `/quote/list` da Brapi, gravada em [file] para a seleção de ativos abrir sem
 * rede.
 *
 * Guarda o DTO da Brapi como chegou, em JSON. O `proguard-rules.pro` mantém as classes de
 * `api.**`, então os nomes dos campos não mudam de um build para outro e a lista gravada por
 * uma versão do app continua legível na seguinte.
 */
internal class AssetListCache(private val file: File) {
    private val gson = Gson()

    /**
     * Substitui a lista salva por [stocks]. Grava num arquivo ao lado e o renomeia, para uma
     * interrupção no meio da gravação não deixar a lista pela metade.
     *
     * @throws IOException quando não consegue gravar ou renomear.
     */
    suspend fun save(stocks: List<StockQuote>) = withContext(Dispatchers.IO) {
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(gson.toJson(QuoteListResponse(stocks)))
        if (!tmp.renameTo(file)) {
            tmp.delete()
            throw IOException("Não renomeou ${tmp.name} para ${file.name}")
        }
    }

    /** Lista salva, vazia quando o arquivo não existe ou não se lê. */
    suspend fun load(): List<StockQuote> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        try {
            // O Gson ignora a nulidade do Kotlin: um arquivo sem `stocks` chega com null.
            gson.fromJson(file.readText(), QuoteListResponse::class.java)?.stocks.orEmpty()
        } catch (e: IOException) {
            Log.w(TAG, "Lista de ativos salva não foi lida", e)
            emptyList()
        } catch (e: JsonParseException) {
            Log.w(TAG, "Lista de ativos salva está corrompida", e)
            emptyList()
        }
    }

    private companion object {
        const val TAG = "AssetListCache"
    }
}
