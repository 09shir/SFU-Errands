package com.example.sfuerrands.data.repository

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Centralized Firebase Storage helper.
 * Stores and retrieves images under:
 *  - profile_photos/{uid}.jpg
 *  - errand_medias/{errandId}/{filename}.jpg
 *  - chat_medias/{errandId}/{messageId}/{filename}.jpg
 */
class StorageRepository {

    private val storage = Firebase.storage
    private val root: StorageReference = storage.reference

    sealed class Dir {
        data class ProfilePhoto(val uid: String) : Dir()
        data class ErrandMedia(val errandId: String) : Dir()
        data class ChatMedia(val errandId: String, val messageId: String) : Dir()
    }

    data class UploadResult(
        val storagePath: String,   // e.g. "errand_medias/abc123/img_...jpg"
        val downloadUrl: String    // https://... tokenized URL
    )

    // ---------- Path helpers ----------

    private fun dirRef(dir: Dir): StorageReference = when (dir) {
        is Dir.ProfilePhoto -> root.child("profile_photos/${dir.uid}.jpg")
        is Dir.ErrandMedia  -> root.child("errand_medias/${dir.errandId}")
        is Dir.ChatMedia    -> root.child("chat_medias/${dir.errandId}/${dir.messageId}")
    }

    private fun makeFileName(ext: String = "jpg"): String =
        "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"

    // ---------- Uploads ----------

    /** Upload from a content Uri (gallery/camera result). */
    suspend fun uploadImage(
        dir: Dir,
        fileUri: Uri,
        fileExtension: String = "jpg",
        contentType: String = "image/jpeg"
    ): UploadResult {
        val (ref, path) = targetRefForUpload(dir, fileExtension)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putFile(fileUri, metadata).await()
        val url = ref.downloadUrl.await().toString()
        return UploadResult(path, url)
    }

    /** Upload from raw bytes (cropped/processed image). */
    suspend fun uploadImageBytes(
        dir: Dir,
        bytes: ByteArray,
        fileExtension: String = "jpg",
        contentType: String = "image/jpeg"
    ): UploadResult {
        val (ref, path) = targetRefForUpload(dir, fileExtension)
        val metadata = com.google.firebase.storage.StorageMetadata.Builder()
            .setContentType(contentType)
            .build()
        ref.putBytes(bytes, metadata).await()
        val url = ref.downloadUrl.await().toString()
        return UploadResult(path, url)
    }

    private fun targetRefForUpload(dir: Dir, fileExtension: String): Pair<StorageReference, String> {
        return when (dir) {
            is Dir.ProfilePhoto -> {
                val ref = dirRef(dir) // fixed path: overwrite per upload
                ref to ref.path.removePrefix("/")
            }
            is Dir.ErrandMedia -> {
                val name = makeFileName(fileExtension)
                val ref = dirRef(dir).child(name)
                ref to ref.path.removePrefix("/")
            }
            is Dir.ChatMedia -> {
                val name = makeFileName(fileExtension)
                val ref = dirRef(dir).child(name)
                ref to ref.path.removePrefix("/")
            }
        }
    }

    // ---------- Deletes & lists ----------

    /** Delete by storage path you saved in Firestore, e.g. "errand_medias/abc/file.jpg". */
    suspend fun deleteByPath(storagePath: String) {
        root.child(storagePath).delete().await()
    }

    /** List files under a folder (errand/chat). For profile photo use exact path. */
    suspend fun listFolder(dir: Dir, maxResults: Int = 50): List<String> {
        val ref = dirRef(dir)
        val result = ref.list(maxResults).await()
        return result.items.map { it.path.removePrefix("/") }
    }
}
