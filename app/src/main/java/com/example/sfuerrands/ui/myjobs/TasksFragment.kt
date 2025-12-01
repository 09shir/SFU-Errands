package com.example.sfuerrands.ui.myjobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.Errand
import com.example.sfuerrands.data.models.ErrandQuery
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.databinding.FragmentTasksBinding
import com.example.sfuerrands.ui.chat.ChatActivity
import com.example.sfuerrands.ui.home.Job
import com.example.sfuerrands.ui.home.JobAdapter
import com.example.sfuerrands.ui.profile.ProfileDisplayActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.sfuerrands.data.repository.ChatRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

// Fragment for tasks you're RUNNING for other people
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val errandRepository = ErrandRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var jobAdapter: JobAdapter
    private var errandsListener: ListenerRegistration? = null
    private var currentErrands: List<Errand> = emptyList()
    private val chatListeners = mutableMapOf<String, ListenerRegistration>()  // Track chat listeners
    private val unreadCounts = mutableMapOf<String, Int>()  // Track unread counts by errand ID

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        listenForMyTasks()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the listener to update unread counts
        errandsListener?.remove()
        listenForMyTasks()
    }

    private fun setupRecyclerView() {
        jobAdapter = JobAdapter(emptyList())

        // Custom click listener to open TaskDetailActivity
        jobAdapter.onJobClickListener = { job ->
            val errand = currentErrands.find { it.id == job.id }

            if (errand != null) {
                val intent = Intent(requireContext(), TaskDetailActivity::class.java)
                intent.putExtra("ERRAND_ID", errand.id)
                intent.putExtra("ERRAND_TITLE", errand.title)
                intent.putExtra("ERRAND_DESCRIPTION", errand.description)
                intent.putExtra("ERRAND_CAMPUS", errand.campus)
                intent.putExtra("ERRAND_PRICE", errand.priceOffered)
                intent.putExtra("ERRAND_LOCATION", errand.location)
                intent.putExtra("ERRAND_RUNNER_COMPLETION", errand.runnerCompletion)
                intent.putStringArrayListExtra("ERRAND_PHOTO_URLS", ArrayList(errand.photoUrls))
                startActivity(intent)
            }
        }

        jobAdapter.onProfileClickListener = { job ->
            val requesterRef = job.requester

            if (requesterRef != null) {
                val intent = Intent(requireContext(), ProfileDisplayActivity::class.java).apply {
                    putExtra("PERSON_PATH", requesterRef.path)
                    putExtra("ROLE", "requester")
                }
                startActivity(intent)
            } else {
                Log.e("TasksFragment", "Cannot open profile: Requester ref is null")
            }
        }

        jobAdapter.onChatClickListener = { job ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_ERRAND_ID, job.id)
                putExtra(ChatActivity.EXTRA_ERRAND_TITLE, job.title)
            }
            startActivity(intent)
        }

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = jobAdapter
        }
    }

    private fun listenForMyTasks() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val myRef = db.collection("users").document(currentUid)
        val chatRepo = ChatRepository()

        errandsListener = errandRepository.listenErrands(
            query = ErrandQuery(
                runnerId = myRef,
                orderByCreatedAtDesc = true
            ),
            onSuccess = { errands ->
                currentErrands = errands
                
                // Remove old chat listeners that are no longer needed
                val currentErrandIds = errands.map { it.id }.toSet()
                chatListeners.keys.toList().forEach { errandId ->
                    if (errandId !in currentErrandIds) {
                        chatListeners[errandId]?.remove()
                        chatListeners.remove(errandId)
                        unreadCounts.remove(errandId)
                    }
                }
                
                // Set up real-time listeners for unread counts
                errands.forEach { errand ->
                    if (!chatListeners.containsKey(errand.id)) {
                        val listener = chatRepo.listenUnreadCount(errand.id, myRef) { count ->
                            unreadCounts[errand.id] = count
                            updateJobsList()
                        }
                        chatListeners[errand.id] = listener
                    }
                }
                
                // Initial update
                updateJobsList()
            },
            onError = { exception ->
                Toast.makeText(
                    requireContext(),
                    "Error: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
    
    private fun updateJobsList() {
        val jobs = currentErrands.map { errand ->
            Job(
                id = errand.id,
                title = errand.title,
                description = errand.description,
                // past data had lowercase campus names
                campus = errand.campus.replaceFirstChar{it.uppercaseChar()},
                location = errand.location?: "N/A",
                payment = errand.priceOffered?.let { "$$it" } ?: "Free",
                mediaPaths = errand.photoUrls,
                isClaimed = errand.status == "claimed" || errand.runnerId != null,
                requester = errand.requesterId,
                runner = errand.runnerId,
                unreadMessageCount = unreadCounts[errand.id] ?: 0
            )
        }
        jobAdapter.submitList(jobs)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errandsListener?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
        _binding = null
    }
}