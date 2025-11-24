package com.example.sfuerrands.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference

data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: DocumentReference? = null,
    val text: String? = null,
    val media: List<String>? = null,
    val createdAt: Timestamp? = null,
    val delivered: Boolean = false,
    val read: Boolean = false
)
