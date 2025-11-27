package com.example.sfuerrands.data.repository

import com.google.firebase.auth.auth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await
import com.example.sfuerrands.data.models.User

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore
    fun isSignedIn(): Boolean = auth.currentUser != null
    suspend fun signOut() { auth.signOut() }

    /**
     * Sign up with email/password, set display name, and create /users/{uid}.
     */
    suspend fun signUpAndSendVerification(
        email: String,
        password: String,
        displayName: String
    ) {
        val cred = auth.createUserWithEmailAndPassword(email, password).await()
        val user = cred.user ?: error("No user from FirebaseAuth")

        val profile = com.google.firebase.auth.userProfileChangeRequest {
            this.displayName = displayName
        }
        user.updateProfile(profile).await()

        user.sendEmailVerification().await()
    }

    // Check if email is verified, after refreshing user data from server.
    suspend fun isEmailVerifiedFresh(): Boolean {
        val u = auth.currentUser ?: return false
        u.reload().await()                 // refresh user profile
        if (!u.isEmailVerified) return false
        u.getIdToken(true).await()         // refresh ID token claims (email_verified)
        return true
    }

    // Create /users/{uid} only after email is verified.
    suspend fun createUserDocIfMissing(
        displayName: String,
        campuses: List<String> = emptyList()
    ) {
        val u = auth.currentUser ?: error("Not signed in")
        u.reload().await()
        require(u.isEmailVerified) { "Email not verified" }
        u.getIdToken(true).await()

        val docRef = db.collection("users").document(u.uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val doc = mapOf(
                "displayName" to (displayName.ifBlank { u.displayName ?: "" }),
                "email" to (u.email ?: ""),
                "photoUrl" to null,
                "campuses" to campuses,
                "requesterRatingCount" to 0,
                "requesterRatingSum" to 0.0,
                "runnerRatingCount" to 0,
                "runnerRatingSum" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastActiveAt" to FieldValue.serverTimestamp()
            )
            docRef.set(doc).await()
        }
    }

    /**
     * Sign in with email/password.
     */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun resendVerificationEmail() {
        val u = auth.currentUser ?: error("Not signed in")
        u.sendEmailVerification().await()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}
