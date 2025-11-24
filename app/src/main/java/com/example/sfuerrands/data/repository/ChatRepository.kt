package com.example.sfuerrands.data.repository

import com.example.sfuerrands.data.models.ChatMessage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val db = Firebase.firestore

    private fun chatCollection(errandId: String) =
        db.collection("errands")
            .document(errandId)
            .collection("chat")

    // Listen to messages for an errand
    fun listenMessages(
        errandId: String,
        onSuccess: (List<ChatMessage>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return chatCollection(errandId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    onError(e); return@addSnapshotListener
                }
                if (snap == null) {
                    onSuccess(emptyList()); return@addSnapshotListener
                }
                val msgs = snap.toObjects(ChatMessage::class.java)
                onSuccess(msgs)
            }
    }

    // Simple text message
    suspend fun sendTextMessage(
        errandId: String,
        senderRef: DocumentReference,
        text: String
    ) {
        val msg = hashMapOf(
            "senderId" to senderRef,
            "text" to text,
            "media" to null,
            "createdAt" to FieldValue.serverTimestamp(),
            "delivered" to false,
            "read" to false
        )
        chatCollection(errandId).add(msg).await()
    }

    // Message that may have text + media URLs
    suspend fun sendMediaMessage(
        errandId: String,
        senderRef: DocumentReference,
        mediaUrls: List<String>,
        text: String? = null
    ) {
        val msg = hashMapOf(
            "senderId" to senderRef,
            "text" to text,
            "media" to mediaUrls,
            "createdAt" to FieldValue.serverTimestamp(),
            "delivered" to false,
            "read" to false
        )
        chatCollection(errandId).add(msg).await()
    }

    // Mark all messages from other user as delivered
    suspend fun markDelivered(errandId: String, myRef: DocumentReference) {
        val snap = chatCollection(errandId)
            .whereNotEqualTo("senderId", myRef)
            .whereEqualTo("delivered", false)
            .get()
            .await()

        db.runBatch { batch ->
            snap.documents.forEach { doc ->
                batch.update(doc.reference, "delivered", true)
            }
        }.await()
    }

    // Mark all messages from other user as read
    suspend fun markRead(errandId: String, myRef: DocumentReference) {
        val snap = chatCollection(errandId)
            .whereNotEqualTo("senderId", myRef)
            .whereEqualTo("read", false)
            .get()
            .await()

        db.runBatch { batch ->
            snap.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "read" to true,
                    "delivered" to true
                ))
            }
        }.await()
    }
}
