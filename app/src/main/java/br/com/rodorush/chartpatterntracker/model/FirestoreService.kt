package br.com.rodorush.chartpatterntracker.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestoreSettings
import kotlinx.coroutines.tasks.await

/**
 * Velas lidas do Firestore.
 *
 * @property fetchedAt instante em que a `updateCandlesticks` leu a série da Brapi, em ms epoch:
 * o maior `fetchedAt` dos documentos lidos. `0` quando nenhum deles traz o campo.
 */
data class RemoteCandlesticks(val candles: List<Candlestick>, val fetchedAt: Long)

class FirestoreService(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    init {
        // Habilitar cache offline (apenas se ainda não configurado)
        if (!db.firestoreSettings.isPersistenceEnabled) {
            db.firestoreSettings = firestoreSettings {
                isPersistenceEnabled = true
            }
        }
    }

    /**
     * Lê de `candlesticks/{ticker}/{timeframe}` as velas com início entre [startTime] e
     * [endTime], em ms epoch, as duas pontas incluídas.
     */
    suspend fun fetchCandlesticks(
        ticker: String,
        timeframe: String,
        startTime: Long,
        endTime: Long
    ): RemoteCandlesticks {
        val snapshot = db.collection("candlesticks")
            .document(ticker)
            .collection(timeframe)
            .whereGreaterThanOrEqualTo("time", startTime)
            .whereLessThanOrEqualTo("time", endTime)
            .orderBy("time", Query.Direction.ASCENDING)
            .get()
            .await()

        val candles = snapshot.documents.mapNotNull { doc ->
            try {
                Candlestick(
                    time = doc.getLong("time") ?: return@mapNotNull null,
                    open = doc.getDouble("open")?.toFloat() ?: return@mapNotNull null,
                    high = doc.getDouble("high")?.toFloat() ?: return@mapNotNull null,
                    low = doc.getDouble("low")?.toFloat() ?: return@mapNotNull null,
                    close = doc.getDouble("close")?.toFloat() ?: return@mapNotNull null,
                    volume = doc.getLong("volume")
                )
            } catch (e: Exception) {
                null
            }
        }
        // O maior, e não o do último documento: a chamada à função regrava todas as velas que
        // a Brapi ainda devolve, e o que ficou com carimbo antigo é vela que ela deixou de
        // devolver.
        val fetchedAt = snapshot.documents.maxOfOrNull { (it.get("fetchedAt") as? Number)?.toLong() ?: 0L } ?: 0L
        return RemoteCandlesticks(candles, fetchedAt)
    }
}