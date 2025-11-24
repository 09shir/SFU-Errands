package com.example.sfuerrands.ui.settings

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ProfileLoader {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun loadProfileInto(viewModel: EditProfileViewModel) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("ProfileLoader", "User not logged in — cannot load profile")
            return
        }

        try {
            Log.d("ProfileLoader", "Loading Firestore profile for uid=$uid")

            val snapshot = db.collection("users").document(uid).get().await()

            if (!snapshot.exists()) {
                Log.e("ProfileLoader", "No Firestore profile exists for this user.")
                return
            }

            // Extract fields safely
            val displayName = snapshot.getString("displayName") ?: "Your Name"
            val email = snapshot.getString("email") ?: "example@sfu.ca"
            val campuses = snapshot.get("campuses") as? List<String> ?: emptyList()
            val photoUrl = snapshot.getString("photoUrl") // may be null

            Log.d("ProfileLoader", "Loaded profile:")
            Log.d("ProfileLoader", "displayName=$displayName")
            Log.d("ProfileLoader", "email=$email")
            Log.d("ProfileLoader", "campuses=$campuses")
            Log.d("ProfileLoader", "photoUrl=$photoUrl")

            // Push into ViewModel
            viewModel.setName(displayName)
            viewModel.setEmail(email)
            viewModel.setCampuses(campuses)
            viewModel.setExistingPhotoUrl(photoUrl)

        } catch (e: Exception) {
            Log.e("ProfileLoader", "Error loading profile: ${e.message}")
            e.printStackTrace()
        }
    }
}
