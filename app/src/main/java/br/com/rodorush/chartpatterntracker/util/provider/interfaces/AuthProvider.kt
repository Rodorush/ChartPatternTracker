package br.com.rodorush.chartpatterntracker.util.provider.interfaces

// Interface para o provedor de autenticação
interface AuthProvider {
    fun signInWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit)
    fun createUserWithEmailAndPassword(email: String, password: String, onResult: (Boolean, String?) -> Unit)
    fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit)
    fun logout(onResult: () -> Unit)

    fun sendEmailVerification(onResult: (Boolean, String?) -> Unit)
    fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit)
    fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit)

    // Exclusão de conta (NF-03e) — exige sessão recente; em falha por
    // requires-recent-login, reautenticar antes de retentar.
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
    fun currentUserEmail(): String?
    fun currentUserDisplayName(): String?
    fun currentUserProviderId(): String?
}