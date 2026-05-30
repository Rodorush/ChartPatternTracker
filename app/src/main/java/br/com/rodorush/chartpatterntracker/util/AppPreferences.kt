package br.com.rodorush.chartpatterntracker.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper simples sobre [SharedPreferences] para flags locais do app.
 * Reaproveita o mesmo store "app_prefs" já registrado no Koin (ver di/AppModule.kt).
 */
class AppPreferences(private val prefs: SharedPreferences) {

    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, value) }

    private companion object {
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }
}
