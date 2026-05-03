package br.com.rodorush.chartpatterntracker

import android.app.Application
import br.com.rodorush.chartpatterntracker.di.appModule
import br.com.rodorush.customlogs.CrashlyticsTree
import br.com.rodorush.customlogs.DebugTree
import br.com.rodorush.customlogs.Log
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ChartPatternApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.plant(if (BuildConfig.DEBUG) DebugTree() else CrashlyticsTree())
        try {
            Log.d(TAG, "Inicializando Firebase App Check")
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val debugProviderFactory = DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugProviderFactory)
            Log.d(TAG, "Firebase App Check configurado com provedor Debug")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao configurar Firebase App Check: ${e.message}", e)
        }
        startKoin {
            androidContext(this@ChartPatternApplication)
            modules(appModule)
        }
    }

    private companion object {
        const val TAG = "ChartPatternApplication"
    }
}
