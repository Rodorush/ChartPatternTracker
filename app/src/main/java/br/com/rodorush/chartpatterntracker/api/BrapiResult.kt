package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

/** Ativo da cotação da Brapi. O Gson deixa `null` o campo que falta na resposta. */
data class BrapiResult(
    @SerializedName("historicalDataPrice") val historicalDataPrice: List<HistoricalData>?
)