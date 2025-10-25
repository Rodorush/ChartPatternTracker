package br.com.rodorush.chartpatterntracker.api

import com.google.gson.annotations.SerializedName

data class BrapiQuoteInfoResponse(
    @SerializedName("results") val results: List<BrapiQuoteInfo>
)

data class BrapiQuoteInfo(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("shortName") val shortName: String?,
    @SerializedName("longName") val longName: String?
)

