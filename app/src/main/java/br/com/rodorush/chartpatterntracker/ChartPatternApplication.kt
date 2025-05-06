package br.com.rodorush.chartpatterntracker

import android.app.Application
import android.util.Log
import br.com.rodorush.chartpatterntracker.di.appModule
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChartPatternApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configurar Firebase App Check com provedor Debug
        try {
            Log.d("ChartPatternApplication", "Inicializando Firebase App Check")
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val debugProviderFactory = DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugProviderFactory)
            Log.d("ChartPatternApplication", "Firebase App Check configurado com provedor Debug")
        } catch (e: Exception) {
            Log.e("ChartPatternApplication", "Erro ao configurar Firebase App Check: ${e.message}", e)
        }
        // Inicializar Koin
        startKoin {
            androidContext(this@ChartPatternApplication)
            modules(appModule)
        }
    }
}