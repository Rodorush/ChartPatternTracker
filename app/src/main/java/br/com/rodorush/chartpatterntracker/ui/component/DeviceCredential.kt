package br.com.rodorush.chartpatterntracker.ui.component

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

/**
 * Monta o intent da tela do sistema que pede o bloqueio de tela do aparelho (PIN, padrão, senha
 * ou biometria, conforme o aparelho). O Perfil o abre antes de excluir a conta e de alterar o
 * e-mail, para confirmar que quem está com o celular é o dono.
 *
 * Usa `createConfirmDeviceCredentialIntent`, deprecado desde o API 29, de propósito: é o único
 * caminho que cobre do minSdk 28 em diante sem dependência nova e sem trocar a `MainActivity`
 * por `FragmentActivity`, e volta por launcher de ActivityResult, que sobrevive à recriação da
 * Activity com a tela aberta.
 *
 * @return `null` quando o aparelho não tem bloqueio de tela. Nesse caso a operação segue sem a
 *   credencial: travar deixaria o usuário sem como excluir a conta pelo app.
 */
internal fun deviceCredentialIntent(context: Context, title: String, description: String): Intent? {
    val keyguard = context.getSystemService(KeyguardManager::class.java) ?: return null
    if (!keyguard.isDeviceSecure) return null
    @Suppress("DEPRECATION")
    return keyguard.createConfirmDeviceCredentialIntent(title, description)
}

/**
 * Pede a credencial do aparelho antes de uma operação sensível: sem [intent], porque o aparelho
 * não tem bloqueio de tela, chama [proceed] na hora; com ele, chama [launch], e quem segue com a
 * operação é o retorno da tela, com a credencial confirmada. Se o [launch] falha, a operação não
 * segue.
 *
 * @return `null` quando a tela abriu ou a operação seguiu; a falha do [launch] quando a tela não
 *   abriu.
 */
internal fun <I : Any> requestDeviceCredential(
    intent: I?,
    launch: (I) -> Unit,
    proceed: () -> Unit
): Exception? {
    if (intent == null) {
        proceed()
        return null
    }
    return try {
        launch(intent)
        null
    } catch (e: Exception) {
        e
    }
}
