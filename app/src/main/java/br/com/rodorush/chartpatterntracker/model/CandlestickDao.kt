package br.com.rodorush.chartpatterntracker.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CandlestickDao {
    @Query("SELECT * FROM candlesticks WHERE ticker = :ticker AND timeframe = :timeframe AND time >= :startTime ORDER BY time")
    suspend fun getCandlesticks(ticker: String, timeframe: String, startTime: Long): List<CandlestickEntity>

    @Query("SELECT MAX(time) FROM candlesticks WHERE ticker = :ticker AND timeframe = :timeframe")
    suspend fun getLatestTimestamp(ticker: String, timeframe: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(candlesticks: List<CandlestickEntity>)

    @Query("DELETE FROM candlesticks WHERE ticker = :ticker AND timeframe = :timeframe AND time < :threshold")
    suspend fun deleteOld(ticker: String, timeframe: String, threshold: Long)
}