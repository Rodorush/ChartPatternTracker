package br.com.rodorush.chartpatterntracker.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Wrapper simples sobre [SharedPreferences] para flags locais do app.
 * Reaproveita o mesmo store "app_prefs" já registrado no Koin (ver di/AppModule.kt).
 */
class AppPreferences(private val prefs: SharedPreferences) {

    /**
     * Contas (uid do Firebase) que deram o "Li e concordo" do gate inicial neste aparelho. Desde a
     * unificação do consentimento (PROD-06), o aceite vale para o aviso de risco **e** para os
     * Termos de Uso / Política de Privacidade — é o registro de consentimento universal (cobre
     * login por e-mail e por Google, que não passa pelo checkbox do cadastro).
     */
    val disclaimerAcceptedUids: Set<String>
        // Cópia: o Set devolvido pelo SharedPreferences não pode ser alterado.
        get() = prefs.getStringSet(KEY_DISCLAIMER_ACCEPTED_UIDS, null)?.toSet().orEmpty()

    /** Registra o aceite da conta [uid] neste aparelho. */
    fun acceptDisclaimer(uid: String) {
        prefs.edit { putStringSet(KEY_DISCLAIMER_ACCEPTED_UIDS, disclaimerAcceptedUids + uid) }
    }

    /** Se a conta [uid] ainda precisa aceitar o aviso de risco neste aparelho. */
    fun needsDisclaimer(uid: String?): Boolean = needsDisclaimer(disclaimerAcceptedUids, uid)

    private companion object {
        const val KEY_DISCLAIMER_ACCEPTED_UIDS = "disclaimer_accepted_uids"
    }
}

/**
 * Decide se o gate do aviso de risco aparece: uma vez por conta. Quem volta à mesma conta, depois
 * do Sair, de uma sessão encerrada fora do app ou de outra conta usada no aparelho, não o vê de
 * novo. Conta nova, inclusive a recriada depois de uma exclusão (o uid muda), vê.
 *
 * @param acceptedUids as contas que já aceitaram neste aparelho.
 * @param currentUid a conta da sessão atual; `null` sem sessão, caso em que o aceite é pedido.
 */
internal fun needsDisclaimer(acceptedUids: Set<String>, currentUid: String?): Boolean =
    currentUid == null || currentUid !in acceptedUids
