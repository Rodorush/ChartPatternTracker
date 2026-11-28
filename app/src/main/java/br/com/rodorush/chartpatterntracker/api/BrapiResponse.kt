package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

/** Resposta da cotação da Brapi. O Gson deixa `null` o campo que falta na resposta. */
data class BrapiResponse(
    @SerializedName("results") val results: List<BrapiResult>?
)