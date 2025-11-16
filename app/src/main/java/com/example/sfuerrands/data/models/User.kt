package com.example.sfuerrands.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,          // storage path
    val campuses: List<String> = emptyList(),
    val requesterRatingCount: Int = 0,
    val requesterRatingSum: Double = 0.0,
    val runnerRatingCount: Int = 0,
    val runnerRatingSum: Double = 0.0,
    val createdAt: Timestamp? = null,
    val lastActiveAt: Timestamp? = null
)