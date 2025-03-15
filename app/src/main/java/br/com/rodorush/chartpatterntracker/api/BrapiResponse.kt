package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

data class BrapiResponse(
    @SerializedName("results") val results: List<BrapiResult>
)