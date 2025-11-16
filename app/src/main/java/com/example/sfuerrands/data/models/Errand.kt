package com.example.sfuerrands.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint

data class Errand (
    @DocumentId val id: String = "",          // Firestore doc id (not in schema, but super helpful)
    val requesterId: DocumentReference? = null,
    val title: String = "",
    val description: String = "",
    val campus: String = "",                  // "burnaby|surrey|vancouver"
    val priceOffered: Double? = null,
    val status: String = "open",              // "open|claimed|in_progress|completed|cancelled"
    val runnerId: DocumentReference? = null,
    val runnerCompletion: Boolean = false,
    val clientCompletion: Boolean = false,
    val createdAt: Timestamp? = null,
    val claimedAt: Timestamp? = null,
    val expectedCompletionAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val location: GeoPoint? = null,
    val medias: List<String> = emptyList()
)