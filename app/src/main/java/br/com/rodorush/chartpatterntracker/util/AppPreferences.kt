package br.com.rodorush.chartpatterntracker.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper simples sobre [SharedPreferences] para flags locais do app.
 * Reaproveita o mesmo store "app_prefs" já registrado no Koin (ver di/AppModule.kt).
 */
class AppPreferences(private val prefs: SharedPreferences) {

    /**
     * Aceite do gate inicial. Desde a unificação do consentimento (PROD-06), representa o
     * "Li e concordo" do aviso de risco **e** dos Termos de Uso / Política de Privacidade —
     * é o registro de consentimento universal (cobre login por e-mail e por Google, que não
     * passa pelo checkbox do cadastro).
     */
    var disclaimerAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLAIMER_ACCEPTED, value) }

    private companion object {
        const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }
}
