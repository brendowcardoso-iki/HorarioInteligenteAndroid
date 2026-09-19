package com.example.data.remote

import android.content.Context
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class FirebaseAuthService(private val context: Context) {

    init {
        ensureFirebaseApp(context)
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            ensureFirebaseApp(context)
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    val currentUser: FirebaseUser?
        get() = try {
            auth?.currentUser
        } catch (e: Exception) {
            null
        }

    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val firebaseAuth = auth
        if (firebaseAuth != null) {
            val listener = FirebaseAuth.AuthStateListener { fa ->
                trySend(fa.currentUser)
            }
            firebaseAuth.addAuthStateListener(listener)
            trySend(firebaseAuth.currentUser)
            awaitClose {
                firebaseAuth.removeAuthStateListener(listener)
            }
        } else {
            trySend(null)
            awaitClose { }
        }
    }

    /**
     * Signs in using Google via Credential Manager and Firebase Auth.
     */
    suspend fun signInWithGoogle(serverClientId: String? = null): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth não está inicializado."))
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOptionBuilder = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)

            if (!serverClientId.isNullOrBlank()) {
                googleIdOptionBuilder.setServerClientId(serverClientId)
            }

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOptionBuilder.build())
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val authCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(authCredential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Fallback / Direct sign in with Firebase Auth credential token.
     */
    suspend fun signInWithIdToken(idToken: String): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth não está inicializado."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Firebase user is null after sign in")
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Sign in anonymously (for guest synchronization or fallback).
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        val firebaseAuth = auth ?: return Result.failure(IllegalStateException("Firebase Auth não está inicializado."))
        return try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user ?: throw IllegalStateException("Anonymous user is null")
            Result.success(user)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Signs out from Firebase Auth.
     */
    fun signOut() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun ensureFirebaseApp(context: Context): FirebaseApp? {
            return try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseApp.getInstance()
                } else {
                    val app = FirebaseApp.initializeApp(context)
                    if (app != null) app else {
                        val options = FirebaseOptions.Builder()
                            .setApplicationId(context.packageName)
                            .setApiKey("AIzaSyMockKeyForRuntimeInit12345")
                            .setProjectId("horario-inteligente")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                    }
                }
            } catch (e: Exception) {
                try {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(context.packageName)
                        .setApiKey("AIzaSyMockKeyForRuntimeInit12345")
                        .setProjectId("horario-inteligente")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                } catch (ex: Exception) {
                    null
                }
            }
        }
    }
}
