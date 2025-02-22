package br.com.rodorush.chartpatterntracker.utils

import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// Função suspensa para obter a URL da imagem do Storage
suspend fun loadImageUrlFromStorage(documentId: String): String? {
    return try {
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val imageRef = storageRef.child("images/$documentId.gif")
        imageRef.downloadUrl.await().toString() // Retorna a URL como String
    } catch (e: Exception) {
        println("Erro ao obter URL: ${e.message}")
        null // Retorna null em caso de erro
    }
}