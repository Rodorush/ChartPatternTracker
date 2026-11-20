package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.customlogs.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest

/**
 * Traduz a exceção do Firebase no código de erro do [AuthProvider] que vai a quem chama. Sessão
 * antiga, rede e excesso de tentativas são reconhecidos pelo tipo; as demais falhas da
 * autenticação, pelo `errorCode` ([firebaseAuthErrorCode]). A mensagem do Firebase é texto
 * livre em inglês e não serve de contrato.
 *
 * @return o código, ou `null` para a falha sem código conhecido, que a tela mostra com o texto
 *   genérico da operação.
 */
internal fun authErrorCode(e: Exception?): String? = when (e) {
    is FirebaseAuthRecentLoginRequiredException -> AuthProvider.REQUIRES_RECENT_LOGIN
    is FirebaseAuthException -> firebaseAuthErrorCode(e.errorCode)
    is FirebaseNetworkException -> AuthProvider.NETWORK
    is FirebaseTooManyRequestsException -> AuthProvider.TOO_MANY_REQUESTS
    else -> null
}

/**
 * Traduz o `errorCode` de uma `FirebaseAuthException` no código do [AuthProvider]. A triagem é
 * pelo `errorCode`, e não pela subclasse, porque a subclasse junta casos que a tela separa: a
 * `FirebaseAuthInvalidCredentialsException` carrega a senha errada, o e-mail malformado e, pela
 * subclasse `FirebaseAuthWeakPasswordException`, a senha fraca. Na reautenticação do Perfil,
 * credencial recusada vira "senha incorreta". Por isso só a credencial recusada vira
 * [AuthProvider.INVALID_CREDENTIAL]: senha fraca, e-mail malformado, conta desativada ou
 * inexistente e conta com outro provedor ficam sem código e caem no texto genérico da operação.
 */
internal fun firebaseAuthErrorCode(errorCode: String): String? = when (errorCode) {
    "ERROR_USER_MISMATCH" -> AuthProvider.USER_MISMATCH
    "ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD" -> AuthProvider.INVALID_CREDENTIAL
    "ERROR_EMAIL_ALREADY_IN_USE" -> AuthProvider.EMAIL_IN_USE
    else -> null
}

class FirebaseAuthProvider : AuthProvider {
    private val auth = FirebaseAuth.getInstance()

    override fun signInWithEmailAndPassword(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        // Grava o nome no cadastro para que apareça no perfil
                        // sem depender de uma alteração posterior.
                        val profileUpdate = userProfileChangeRequest { this.displayName = displayName }
                        user.updateProfile(profileUpdate)
                            .addOnCompleteListener { profileTask ->
                                if (!profileTask.isSuccessful) {
                                    Log.w(
                                        TAG,
                                        "createUser: falha ao definir displayName",
                                        profileTask.exception
                                    )
                                }
                                user.sendEmailVerification()
                                    .addOnCompleteListener { verifyTask ->
                                        if (!verifyTask.isSuccessful) {
                                            Log.w(
                                                TAG,
                                                "createUser: falha ao enviar e-mail de verificação",
                                                verifyTask.exception
                                            )
                                        }
                                        onResult(true, null)
                                    }
                            }
                    } else {
                        onResult(true, null)
                    }
                } else {
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == null) {
            // Com Throwable, para virar evento no Crashlytics: idToken nulo indica
            // configuração errada (WEB_CLIENT_ID), não falha do usuário.
            Log.w(TAG, "signInWithGoogle: idToken nulo", IllegalStateException("idToken nulo"))
            onResult(false, AuthProvider.MISSING_ID_TOKEN)
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "signInWithGoogle: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun logout(onResult: () -> Unit) {
        auth.signOut()
        onResult()
    }

    override fun sendEmailVerification(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "sendEmailVerification: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        user.reload()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reloadCurrentUser: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun sendPasswordResetEmail(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "sendPasswordResetEmail: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    logSensitiveFailure("deleteCurrentUser", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun reauthenticateWithPassword(
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email
        if (user == null || email == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reauthenticateWithPassword: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == null) {
            onResult(false, AuthProvider.MISSING_ID_TOKEN)
            return
        }
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reauthenticateWithGoogle: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    logSensitiveFailure("updatePassword", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    logSensitiveFailure("verifyBeforeUpdateEmail", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun updateDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, AuthProvider.NO_CURRENT_USER)
            return
        }
        val request = userProfileChangeRequest { this.displayName = displayName }
        user.updateProfile(request)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "updateDisplayName: falha", task.exception)
                    onResult(false, authErrorCode(task.exception))
                }
            }
    }

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun currentUserEmail(): String? = auth.currentUser?.email

    override fun currentUserDisplayName(): String? = auth.currentUser?.displayName

    override fun currentUserProviderIds(): List<String> =
        auth.currentUser?.providerData.orEmpty()
            .map { it.providerId }
            .filter { it != FIREBASE_PROVIDER }

    override fun currentSignInProvider(onResult: (String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(null)
            return
        }
        // O provedor vem da claim sign_in_provider do token. Com o token vencido, getIdToken
        // precisa de rede; sem ela, quem chama decide pelos provedores vinculados.
        user.getIdToken(false)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(task.result?.signInProvider)
                } else {
                    val e = task.exception
                    if (e is FirebaseNetworkException) {
                        Log.i(TAG, "currentSignInProvider: sem rede para ler o token")
                    } else {
                        Log.w(TAG, "currentSignInProvider: falha", e)
                    }
                    onResult(null)
                }
            }
    }

    /**
     * Registra a falha de uma operação que exige sessão recente. A sessão antiga é caminho
     * esperado, porque o app pede reautenticação. Por isso ela fica em `Log.i`, sem a exceção,
     * para não gerar evento no Crashlytics a cada tentativa. As demais falhas seguem em `Log.w`
     * com a exceção.
     */
    private fun logSensitiveFailure(operation: String, e: Exception?) {
        if (e is FirebaseAuthRecentLoginRequiredException) {
            Log.i(TAG, "$operation: sessão antiga, pede reautenticação")
        } else {
            Log.w(TAG, "$operation: falha", e)
        }
    }

    private companion object {
        const val TAG = "FirebaseAuthProvider"

        // O providerData inclui uma entrada do próprio Firebase, que não é provedor de login.
        const val FIREBASE_PROVIDER = "firebase"
    }
}
