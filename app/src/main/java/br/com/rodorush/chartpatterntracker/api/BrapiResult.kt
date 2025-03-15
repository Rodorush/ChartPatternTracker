package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

data class BrapiResult(
    @SerializedName("historicalDataPrice") val historicalDataPrice: List<HistoricalData>
)