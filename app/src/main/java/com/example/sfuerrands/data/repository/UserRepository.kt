package com.example.sfuerrands.data.repository

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private fun uid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be logged in")
    }

    private fun userDoc() = db.collection("users").document(uid())

    /** Upload image and return download URL */
    suspend fun uploadProfilePhoto(uri: Uri): String {
        val ref = storage.reference.child("profilePhotos/${uid()}.jpg")

        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        Log.d("UserRepository", "Uploaded photo URL: $url")
        return url
    }

    /** Update only the changed fields */
    suspend fun updateProfile(
        displayName: String,
        campuses: List<String>,
        photoUrl: String?
    ) {
        val updateMap = mutableMapOf<String, Any?>(
            "displayName" to displayName,
            "campuses" to campuses
        )

        // If user did NOT upload a new photo, do NOT include photoUrl field.
        if (photoUrl != null) {
            updateMap["photoUrl"] = photoUrl
        }

        userDoc().set(updateMap, SetOptions.merge()).await()

        // Also update last active timestamp
        userDoc().update("lastActiveAt", Timestamp.now())
            .await()

        Log.d("UserRepository", "Profile updated successfully")
    }
}
