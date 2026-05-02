package br.com.rodorush.chartpatterntracker.util

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

suspend fun loadImageUrlFromStorage(documentId: String): String? {
    return try {
        val imageRef = FirebaseStorage.getInstance().reference.child("images/$documentId.gif")
        imageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.w("LoadPatternImage", "Falha ao obter URL para $documentId: ${e.message}")
        null
    }
}