package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.api.BrapiApiService.QuoteListResponse
import br.com.rodorush.chartpatterntracker.api.BrapiApiService.StockQuote
import br.com.rodorush.customlogs.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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

    // O provider é single no Koin: a gravação de uma tela pode seguir em IO depois de a tela
    // sair, enquanto a seguinte grava de novo no mesmo temporário.
    private val mutex = Mutex()

    /**
     * Substitui a lista salva por [stocks]. Grava num arquivo ao lado, força a gravação no disco
     * e o renomeia, para uma interrupção no meio não deixar a lista pela metade.
     *
     * @throws IOException quando não consegue gravar ou renomear.
     */
    suspend fun save(stocks: List<StockQuote>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            FileOutputStream(tmp).use { out ->
                out.write(gson.toJson(QuoteListResponse(stocks)).toByteArray(Charsets.UTF_8))
                out.fd.sync()
            }
            if (!tmp.renameTo(file)) {
                tmp.delete()
                throw IOException("Não renomeou ${tmp.name} para ${file.name}")
            }
        }
    }

    /** Lista salva, vazia quando o arquivo não existe ou não se lê. */
    suspend fun load(): List<StockQuote> = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext emptyList()
            // O bloco não suspende, então o catch amplo não alcança cancelamento: qualquer
            // defeito do arquivo vira lista vazia, como a falha da Brapi sem cópia salva.
            try {
                // O Gson ignora a nulidade do Kotlin: um arquivo sem `stocks` chega com null.
                gson.fromJson(file.readText(), QuoteListResponse::class.java)?.stocks.orEmpty()
            } catch (e: Exception) {
                Log.w(TAG, "Lista de ativos salva não foi lida", e)
                emptyList()
            }
        }
    }

    private companion object {
        const val TAG = "AssetListCache"
    }
}
