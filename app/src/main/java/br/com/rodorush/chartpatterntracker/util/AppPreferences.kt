package br.com.rodorush.chartpatterntracker.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper simples sobre [SharedPreferences] para flags locais do app.
 * Reaproveita o mesmo store "app_prefs" já registrado no Koin (ver di/AppModule.kt).
 */
class AppPreferences(private val prefs: SharedPreferences) {

    /**
     * Conta (uid do Firebase) que deu o "Li e concordo" do gate inicial neste aparelho. Desde a
     * unificação do consentimento (PROD-06), o aceite vale para o aviso de risco **e** para os
     * Termos de Uso / Política de Privacidade — é o registro de consentimento universal (cobre
     * login por e-mail e por Google, que não passa pelo checkbox do cadastro).
     */
    var disclaimerAcceptedUid: String?
        get() = prefs.getString(KEY_DISCLAIMER_ACCEPTED_UID, null)
        set(value) = prefs.edit { putString(KEY_DISCLAIMER_ACCEPTED_UID, value) }

    /** Se a conta [uid] ainda precisa aceitar o aviso de risco neste aparelho. */
    fun needsDisclaimer(uid: String?): Boolean = needsDisclaimer(disclaimerAcceptedUid, uid)

    private companion object {
        const val KEY_DISCLAIMER_ACCEPTED_UID = "disclaimer_accepted_uid"
    }
}

/**
 * Decide se o gate do aviso de risco aparece: uma vez por conta. Quem volta à mesma conta, depois
 * do Sair ou de uma sessão encerrada fora do app, não o vê de novo. Conta nova, inclusive a
 * recriada depois de uma exclusão (o uid muda), vê.
 *
 * @param acceptedUid a conta que já aceitou neste aparelho; `null` se nenhuma aceitou.
 * @param currentUid a conta da sessão atual; `null` sem sessão, caso em que o aceite é pedido.
 */
internal fun needsDisclaimer(acceptedUid: String?, currentUid: String?): Boolean =
    currentUid == null || acceptedUid != currentUid
