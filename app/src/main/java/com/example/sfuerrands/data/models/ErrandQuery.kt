package com.example.sfuerrands.data.models

import com.google.firebase.firestore.DocumentReference

data class ErrandQuery(
    val status: String? = null,
    val campus: String? = null,
    val requesterId: DocumentReference? = null,    // Changed from String
    val runnerId: DocumentReference? = null,       // Changed from String
    val limit: Long? = null,
    val orderByCreatedAtDesc: Boolean = false,
    val orderByCreatedAtAsc: Boolean = false
)