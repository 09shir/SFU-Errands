package com.example.sfuerrands.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Errand (
    @DocumentId val id: String = "",
    val requesterId: String = "",              // User UID who created the errand
    val title: String = "",
    val description: String = "",
    val location: String? = null,              // Optional location as string
    val campus: String = "",                   // "burnaby", "surrey", or "vancouver"
    val priceOffered: Double? = null,
    val status: String = "open",               // "open", "claimed", "in_progress", "completed", "cancelled"
    val runnerId: String? = null,              // User UID who accepted the errand
    val runnerCompletion: Boolean = false,
    val clientCompletion: Boolean = false,
    val createdAt: Timestamp? = null,
    val claimedAt: Timestamp? = null,
    val expectedCompletionAt: Timestamp? = null,
    val photoUrls: List<String> = emptyList()  // Array of photo URLs
)