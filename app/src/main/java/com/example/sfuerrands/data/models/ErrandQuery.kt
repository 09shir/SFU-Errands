package com.example.sfuerrands.data.models

// for fetching errands with filters
data class ErrandQuery(
    val status: String? = null,
    val campus: String? = null,
    val requesterId: String? = null,
    val runnerId: String? = null,
    val limit: Long? = null,
    val orderByCreatedAtDesc: Boolean = false,
    val orderByCreatedAtAsc: Boolean = false
)
