package com.example.sfuerrands.data.repository

import com.google.firebase.auth.auth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    fun currentUid(): String? = auth.currentUser?.uid
    fun isSignedIn(): Boolean = auth.currentUser != null
    suspend fun signOut() { auth.signOut() }

    /**
     * Sign up with email/password, set display name, and create /users/{uid}.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
        campuses: List<String> = emptyList()
    ) {
        val cred = auth.createUserWithEmailAndPassword(email, password).await()
        val user = cred.user ?: error("No user from FirebaseAuth")

        // Optional: update profile display name
        val profile = com.google.firebase.auth.userProfileChangeRequest {
            this.displayName = displayName
        }
        user.updateProfile(profile).await()

        // Create /users/{uid} matching your schema
        val uid = user.uid
        val doc = mapOf(
            "displayName" to displayName,
            "email" to email,
            "photoUrl" to null,               // storage path later
            "campuses" to campuses,           // e.g., listOf("burnaby")
            "requesterRatingCount" to 0,
            "requesterRatingSum" to 0.0,
            "runnerRatingCount" to 0,
            "runnerRatingSum" to 0.0,
            "createdAt" to FieldValue.serverTimestamp(),
            "lastActiveAt" to FieldValue.serverTimestamp()
        )
        db.collection("users").document(uid).set(doc).await()

        // Optional: email verification
        // user.sendEmailVerification().await()
    }

    /**
     * Sign in with email/password.
     */
    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }
}
