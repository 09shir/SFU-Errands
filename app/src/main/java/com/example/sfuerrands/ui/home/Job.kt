package com.example.sfuerrands.ui.home

data class Job(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val payment: String,
    val mediaPaths: List<String> = emptyList(),
    val isClaimed: Boolean = false  // NEW: Track if job is claimed
)