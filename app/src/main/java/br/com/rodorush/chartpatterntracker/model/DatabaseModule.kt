package br.com.rodorush.chartpatterntracker.model

import android.content.Context
import androidx.room.Room

object DatabaseModule {
    private var appDatabase: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        return appDatabase ?: synchronized(this) {
            appDatabase ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "chart_pattern_tracker_db"
            ).build().also { appDatabase = it }
        }
    }

    fun provideCandlestickDao(database: AppDatabase): CandlestickDao {
        return database.candlestickDao()
    }
}