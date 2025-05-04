package br.com.rodorush.chartpatterntracker.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import br.com.rodorush.chartpatterntracker.model.AppDatabase
import br.com.rodorush.chartpatterntracker.model.CandlestickDao
import br.com.rodorush.chartpatterntracker.model.CandlestickRepository
import br.com.rodorush.chartpatterntracker.model.FirestoreService
import br.com.rodorush.chartpatterntracker.ui.viewmodel.ChartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "chart_pattern_db"
        ).build()
    }

    single { get<AppDatabase>().candlestickDao() }

    single { FirestoreService() }

    single { CandlestickRepository(get<CandlestickDao>(), get<FirestoreService>()) }

    single<SharedPreferences> {
        androidContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    viewModel {
        ChartViewModel(
            preferences = get<SharedPreferences>(),
            repository = get<CandlestickRepository>()
        )
    }
}