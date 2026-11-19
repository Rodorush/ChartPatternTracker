package br.com.rodorush.chartpatterntracker.util.provider

import br.com.rodorush.chartpatterntracker.util.provider.interfaces.AuthProvider
import br.com.rodorush.customlogs.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest

/**
 * Erro que as operações que exigem sessão recente devolvem a quem chama. A sessão antiga é
 * reconhecida pelo tipo da exceção e vira [AuthProvider.REQUIRES_RECENT_LOGIN], porque a
 * mensagem do Firebase é texto livre em inglês e não serve de contrato. As demais falhas
 * seguem com a mensagem crua.
 */
internal fun sensitiveOperationError(e: Exception?): String? =
    if (e is FirebaseAuthRecentLoginRequiredException) AuthProvider.REQUIRES_RECENT_LOGIN else e?.message

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
                    onResult(false, task.exception?.message)
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
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun signInWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == null) {
            // Com Throwable, para virar evento no Crashlytics: idToken nulo indica
            // configuração errada (WEB_CLIENT_ID), não falha do usuário.
            Log.w(TAG, "signInWithGoogle: idToken nulo", IllegalStateException("idToken nulo"))
            onResult(false, "ID Token nulo ou inválido.")
            return
        }

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "signInWithGoogle: falha", task.exception)
                    onResult(false, task.exception?.message)
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
            onResult(false, "no_current_user")
            return
        }
        user.sendEmailVerification()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "sendEmailVerification: falha", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun reloadCurrentUser(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        user.reload()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reloadCurrentUser: falha", task.exception)
                    onResult(false, task.exception?.message)
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
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun deleteCurrentUser(onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        user.delete()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "deleteCurrentUser: falha", task.exception)
                    onResult(false, sensitiveOperationError(task.exception))
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
            onResult(false, "no_current_user")
            return
        }
        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reauthenticateWithPassword: falha", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun reauthenticateWithGoogle(idToken: String?, onResult: (Boolean, String?) -> Unit) {
        if (idToken == null) {
            onResult(false, "ID Token nulo ou inválido.")
            return
        }
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "reauthenticateWithGoogle: falha", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "updatePassword: falha", task.exception)
                    onResult(false, sensitiveOperationError(task.exception))
                }
            }
    }

    override fun verifyBeforeUpdateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "verifyBeforeUpdateEmail: falha", task.exception)
                    onResult(false, sensitiveOperationError(task.exception))
                }
            }
    }

    override fun updateDisplayName(displayName: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onResult(false, "no_current_user")
            return
        }
        val request = userProfileChangeRequest { this.displayName = displayName }
        user.updateProfile(request)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    Log.w(TAG, "updateDisplayName: falha", task.exception)
                    onResult(false, task.exception?.message)
                }
            }
    }

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun currentUserEmail(): String? = auth.currentUser?.email

    override fun currentUserDisplayName(): String? = auth.currentUser?.displayName

    override fun currentUserProviderId(): String? =
        auth.currentUser?.providerData
            ?.firstOrNull { it.providerId != "firebase" }
            ?.providerId

    private companion object {
        const val TAG = "FirebaseAuthProvider"
    }
}
