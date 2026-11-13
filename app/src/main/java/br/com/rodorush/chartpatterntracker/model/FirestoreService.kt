package br.com.rodorush.chartpatterntracker.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestoreSettings
import kotlinx.coroutines.tasks.await

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

    suspend fun fetchCandlesticks(
        ticker: String,
        timeframe: String,
        startTime: Long,
        endTime: Long
    ): List<Candlestick> {
        val snapshot = db.collection("candlesticks")
            .document(ticker)
            .collection(timeframe)
            .whereGreaterThanOrEqualTo("time", startTime)
            .whereLessThanOrEqualTo("time", endTime)
            .orderBy("time", Query.Direction.ASCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
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
    }
}