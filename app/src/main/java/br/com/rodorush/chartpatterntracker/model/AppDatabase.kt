package br.com.rodorush.chartpatterntracker.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CandlestickEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun candlestickDao(): CandlestickDao
}