package com.example.sfuerrands.data.repository

import com.example.sfuerrands.data.models.Errand
import com.example.sfuerrands.data.models.ErrandQuery
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await

class ErrandRepository {

    private val db = Firebase.firestore
    private val errandsCollection = db.collection("errands")

    // ---------- LISTEN / FETCH ----------

    /**
     * Listen to errands that match the given query (real-time updates).
     *
     * Returns a ListenerRegistration – call .remove() when you no longer need it.
     */
    fun listenErrands(
        query: ErrandQuery,
        onSuccess: (List<Errand>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        var q: Query = errandsCollection

        // Apply filters only if provided
        query.status?.let { q = q.whereEqualTo("status", it) }
        query.campus?.let { q = q.whereEqualTo("campus", it) }
        query.requesterId?.let { q = q.whereEqualTo("requesterId", it) }
        query.runnerId?.let { q = q.whereEqualTo("runnerId", it) }

        // Ordering
        q = if (query.orderByCreatedAtDesc) {
            q.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            q.orderBy("createdAt", Query.Direction.ASCENDING)
        }

        // Optional limit
        query.limit?.let { q = q.limit(it) }

        return q.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                onSuccess(emptyList())
                return@addSnapshotListener
            }

            val errands = snapshot.toObjects(Errand::class.java)
            onSuccess(errands)
        }
    }

    /**
     * One-time fetch (no live updates).
     */
    suspend fun getErrands(query: ErrandQuery): List<Errand> {
        var q: Query = errandsCollection

        query.status?.let { q = q.whereEqualTo("status", it) }
        query.campus?.let { q = q.whereEqualTo("campus", it) }
        query.requesterId?.let { q = q.whereEqualTo("requesterId", it) }
        query.runnerId?.let { q = q.whereEqualTo("runnerId", it) }

        q = if (query.orderByCreatedAtDesc) {
            q.orderBy("createdAt", Query.Direction.DESCENDING)
        } else {
            q.orderBy("createdAt", Query.Direction.ASCENDING)
        }

        query.limit?.let { q = q.limit(it) }

        val snapshot = q.get().await()
        return snapshot.toObjects(Errand::class.java)
    }

    suspend fun getErrandById(id: String): Errand? {
        val snap = errandsCollection.document(id).get().await()
        return snap.toObject(Errand::class.java)
    }

    /**
     * Create a new errand. Returns the generated document ID.
     */
    suspend fun createErrand(errand: Errand): String {
        val now = Timestamp.now()
        val toSave = errand.copy(
            createdAt = now,
            updatedAt = now
        )
        val docRef = errandsCollection.add(toSave).await()
        return docRef.id
    }

    /**
     * Update arbitrary fields of an errand.
     */
    suspend fun updateErrand(id: String, updates: Map<String, Any?>) {
        val merged = updates.toMutableMap()
        merged["updatedAt"] = Timestamp.now()
        errandsCollection.document(id).update(merged as Map<String, Any>).await()
    }

    /**
     * Delete an errand.
     */
    suspend fun deleteErrand(id: String) {
        errandsCollection.document(id).delete().await()
    }

    /**
     * Example: claim an errand (runner accepts it) – simple version, not using transactions yet.
     */
    suspend fun claimErrand(id: String, runnerId: String) {
        updateErrand(
            id,
            mapOf(
                "runnerId" to runnerId,
                "status" to "claimed"
            )
        )
    }

    /**
     * Listen to a single errand doc and get real-time updates.
     *
     * @param includeLocal Whether to receive local (pending writes) events too.
     *                     If false, only server-committed updates trigger.
     */
    fun listenErrandById(
        id: String,
        includeLocal: Boolean = false,
        onChange: (errand: Errand?, hasPendingWrites: Boolean) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val docRef = errandsCollection.document(id)
        val metadataMode = if (includeLocal) MetadataChanges.INCLUDE else MetadataChanges.EXCLUDE

        return docRef.addSnapshotListener(metadataMode) { snap: DocumentSnapshot?, e ->
            if (e != null) { onError(e); return@addSnapshotListener }

            val hasPending = snap?.metadata?.hasPendingWrites() == true
            if (snap != null && snap.exists()) {
                onChange(snap.toObject(Errand::class.java), hasPending)
            } else {
                // Document was deleted or never existed
                onChange(null, hasPending)
            }
        }
    }

// Example usage of listenErrandById() in an Activity/Fragment:

//    private var errandReg: ListenerRegistration? = null
//
//    override fun onStart() {
//        super.onStart()
//        errandReg = errandRepository.listenErrandById(
//            id = errandId,
//            includeLocal = true,
//            onChange = { errand, pending ->
//                // Update UI with latest errand
//                // e.g., titleView.text = errand?.title ?: "(deleted)"
//            },
//            onError = { e -> Log.e("Errand", "Listen error", e) }
//        )
//    }
//
//    override fun onStop() {
//        super.onStop()
//        errandReg?.remove()
//        errandReg = null
//    }
}
