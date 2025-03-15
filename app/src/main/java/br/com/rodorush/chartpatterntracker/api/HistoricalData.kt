package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

data class HistoricalData(
    @SerializedName("date") val date: Long,
    @SerializedName("open") val open: Double,
    @SerializedName("high") val high: Double,
    @SerializedName("low") val low: Double,
    @SerializedName("close") val close: Double,
    @SerializedName("volume") val volume: Long
)