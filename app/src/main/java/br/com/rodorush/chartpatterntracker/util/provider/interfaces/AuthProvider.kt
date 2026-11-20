package br.com.rodorush.chartpatterntracker.util.provider.interfaces

/**
 * Provedor de autenticação. Nas operações com `onResult: (Boolean, String?)`, o erro devolvido
 * na falha é um dos códigos do `companion object` ou `null` (falha sem código conhecido), nunca
 * a mensagem do provedor: ela é texto livre em inglês e não serve de contrato nem de texto de
 * tela. Quem mostra o erro ao usuário traduz o código para `R.string`.
 */
interface AuthProvider {
    fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit)
    fun createUserWithEmailAndPassword(email: String, password: String, displayName: String, onResult: (Boolean, String?) -> Unit)
    fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit)
    fun logout(onResult: () -> Unit)

    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit)
    fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit)
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit)

    // Exclusão de conta (NF-03e) — exige sessão recente; em falha por
    // REQUIRES_RECENT_LOGIN, reautenticar antes de retentar.
    fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit)
    fun reauthenticateWithPassword(password: String, onResult: (Boolean, String?) -> Unit)
    fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit)

    // Gestão de conta (NF-03b/c/d) — autenticado.
    // updatePassword e verifyBeforeUpdateEmail exigem sessão recente (igual a delete);
    // updateDisplayName não exige. updateEmail foi descontinuado — usar
    // verifyBeforeUpdateEmail, que envia link ao novo endereço e só efetiva após o clique.
    fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit)
    fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit)
    fun updateDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit)

    fun isEmailVerified(): Boolean
    fun currentUserId(): String?
    fun currentUserEmail(): String?
    fun currentUserDisplayName(): String?

    /**
     * Provedores vinculados à conta da sessão ([PASSWORD_PROVIDER], [GOOGLE_PROVIDER]), lidos da
     * sessão local, sem rede. Uma conta pode ter mais de um; a ordem não diz por qual o usuário
     * entrou (ver [currentSignInProvider]).
     *
     * @return lista vazia sem sessão.
     */
    fun currentUserProviderIds(): List<String>

    /**
     * Provedor pelo qual a sessão atual foi aberta ou reautenticada pela última vez, como
     * [PASSWORD_PROVIDER] ou [GOOGLE_PROVIDER]. Pode precisar de rede para renovar o token.
     *
     * @param onResult recebe `null` sem sessão ou quando o provedor não pôde ser lido.
     */
    fun currentSignInProvider(onResult: (String?) -> Unit)

    companion object {
        /** Provedor de e-mail e senha. */
        const val PASSWORD_PROVIDER = "password"

        /** Provedor Google. */
        const val GOOGLE_PROVIDER = "google.com"

        /**
         * Erro que [deleteCurrentUser], [updatePassword] e [verifyBeforeUpdateEmail] devolvem
         * quando a sessão é antiga demais para a operação: quem chama reautentica e retenta.
         */
        const val REQUIRES_RECENT_LOGIN = "requires-recent-login"

        /** A operação pede usuário logado, e não há sessão. */
        const val NO_CURRENT_USER = "no-current-user"

        /** O login ou a reautenticação com Google chegou sem idToken. */
        const val MISSING_ID_TOKEN = "missing-id-token"

        /** Credencial recusada: senha errada, ou credencial malformada ou vencida. */
        const val INVALID_CREDENTIAL = "invalid-credential"

        /** A reautenticação usou uma conta diferente da sessão atual. */
        const val USER_MISMATCH = "user-mismatch"

        /** O e-mail já pertence a outra conta. */
        const val EMAIL_IN_USE = "email-in-use"

        /** Falha de rede ao falar com o provedor. */
        const val NETWORK = "network"

        /** O provedor bloqueou temporariamente por excesso de tentativas. */
        const val TOO_MANY_REQUESTS = "too-many-requests"
    }
}