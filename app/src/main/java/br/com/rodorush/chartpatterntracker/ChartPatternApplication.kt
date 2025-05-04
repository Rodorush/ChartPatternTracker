package br.com.rodorush.chartpatterntracker

import android.app.Application
import br.com.rodorush.chartpatterntracker.di.appModule
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChartPatternApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)
        startKoin {
            androidContext(this@ChartPatternApplication)
            modules(appModule)
        }
    }
}