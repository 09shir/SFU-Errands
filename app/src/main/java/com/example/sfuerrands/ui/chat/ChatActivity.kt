package com.example.sfuerrands.ui.chat

import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.ChatMessage
import com.example.sfuerrands.data.repository.ChatRepository
import com.example.sfuerrands.data.repository.StorageRepository
import com.example.sfuerrands.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatRepo = ChatRepository()
    private val storageRepo = StorageRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var adapter: ChatAdapter

    private var errandId: String = ""
    private var errandTitle: String = ""

    private val selectedMedia = mutableListOf<Uri>()
    private val maxMedia = 3

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult

            val remaining = maxMedia - selectedMedia.size
            if (remaining <= 0) {
                Toast.makeText(this, "Max $maxMedia images", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }

            val toAdd = uris.take(remaining)
            selectedMedia.addAll(toAdd)
            updateSelectedMediaUi()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        errandId = intent.getStringExtra(EXTRA_ERRAND_ID) ?: ""
        errandTitle = intent.getStringExtra(EXTRA_ERRAND_TITLE) ?: "Chat"

        if (errandId.isBlank()) {
            Toast.makeText(this, "Missing errand id", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat – $errandTitle"
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = ChatAdapter(auth.currentUser?.uid)
        binding.chatRecycler.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = this@ChatActivity.adapter
        }

        binding.btnAttach.setOnClickListener {
            pickImages.launch("image/*")
        }

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
    }

    private fun listenForMessages() {
        val currentUid = auth.currentUser?.uid ?: return
        val myRef = db.collection("users").document(currentUid)

        chatRepo.listenMessages(
            errandId = errandId,
            onSuccess = { msgs ->
                adapter.submit(msgs)
                binding.chatRecycler.scrollToPosition(msgs.size.coerceAtLeast(1) - 1)

                lifecycleScope.launch {
                    chatRepo.markDelivered(errandId, myRef)
                    chatRepo.markRead(errandId, myRef)
                }
            },
            onError = { e ->
                Toast.makeText(this, "Chat error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty() && selectedMedia.isEmpty()) {
            return
        }

        val uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }
        val senderRef = db.collection("users").document(uid)

        binding.btnSend.isEnabled = false
        binding.btnAttach.isEnabled = false
        binding.progressSending.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val mediaUrls: List<String> =
                    if (selectedMedia.isNotEmpty()) {
                        selectedMedia.map { uri ->
                            async { storageRepo.uploadChatMedia(errandId, uri) }
                        }.awaitAll()
                    } else emptyList()

                if (mediaUrls.isEmpty()) {
                    chatRepo.sendTextMessage(errandId, senderRef, text)
                } else {
                    chatRepo.sendMediaMessage(errandId, senderRef, mediaUrls, text.ifBlank { null })
                }

                binding.messageInput.setText("")
                selectedMedia.clear()
                updateSelectedMediaUi()

            } catch (e: Exception) {
                Toast.makeText(this@ChatActivity, "Send failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            } finally {
                binding.btnSend.isEnabled = true
                binding.btnAttach.isEnabled = true
                binding.progressSending.visibility = View.GONE
            }
        }
    }

    private fun updateSelectedMediaUi() {
        if (selectedMedia.isEmpty()) {
            binding.selectedMediaLabel.visibility = View.GONE
            binding.selectedMediaCount.visibility = View.GONE
        } else {
            binding.selectedMediaLabel.visibility = View.VISIBLE
            binding.selectedMediaCount.visibility = View.VISIBLE
            binding.selectedMediaCount.text = "${selectedMedia.size}/$maxMedia"
        }
    }

    companion object {
        const val EXTRA_ERRAND_ID = "extra_errand_id"
        const val EXTRA_ERRAND_TITLE = "extra_errand_title"
    }
}
