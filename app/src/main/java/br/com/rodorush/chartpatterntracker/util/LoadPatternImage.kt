package br.com.rodorush.chartpatterntracker.util

import br.com.rodorush.customlogs.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

private const val TAG = "LoadPatternImage"

suspend fun loadImageUrlFromStorage(documentId: String): String? {
    return try {
        val imageRef = FirebaseStorage.getInstance().reference.child("images/$documentId.gif")
        imageRef.downloadUrl.await().toString()
    } catch (e: Exception) {
        Log.w(TAG, "Falha ao obter URL para $documentId: ${e.message}")
        null
    }
}