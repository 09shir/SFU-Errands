package com.example.sfuerrands.ui.home

import com.google.firebase.firestore.DocumentReference

data class Job(
    val id: String,
    val title: String,
    val description: String,
    val location: String,
    val campus: String,
    val payment: String,
    val mediaPaths: List<String> = emptyList(),
    val isClaimed: Boolean = false,
    val requester: DocumentReference? = null,
    val runner: DocumentReference? = null,
    val unreadMessageCount: Int = 0,

    // [NEW] List of offers for the UI to display in the dropdown
    val offers: List<DocumentReference> = emptyList()
)