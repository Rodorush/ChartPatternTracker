package br.com.rodorush.chartpatterntracker.model

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestoreSettings
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    suspend fun saveCandlesticks(ticker: String, timeframe: String, candlesticks: List<Candlestick>) {
        val batch = db.batch()
        candlesticks.forEach { candlestick ->
            val docRef = db.collection("candlesticks")
                .document(ticker)
                .collection(timeframe)
                .document(candlestick.time.toString())
            batch.set(docRef, mapOf(
                "time" to candlestick.time,
                "open" to candlestick.open,
                "high" to candlestick.high,
                "low" to candlestick.low,
                "close" to candlestick.close,
                "volume" to candlestick.volume
            ))
        }
        batch.commit().await()
    }

    fun timestampToDateString(timestamp: Long): String {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(date)
    }
}