package br.com.rodorush.chartpatterntracker.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "candlesticks",
    indices = [
        Index(value = ["ticker", "timeframe"]),
        Index(value = ["ticker", "timeframe", "time"], unique = true)
    ]
)
data class CandlestickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ticker: String,
    val timeframe: String,
    val time: Long,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Long?,
    val lastUpdated: Long
)