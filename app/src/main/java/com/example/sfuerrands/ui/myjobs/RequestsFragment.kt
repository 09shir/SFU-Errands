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
import com.example.sfuerrands.databinding.FragmentRequestsBinding
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

// Fragment for the users requests in myJobs
class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

    private val errandRepository = ErrandRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var jobAdapter: JobAdapter
    private var errandsListener: ListenerRegistration? = null

    // Store the full Errand objects so we can pass them to EditJobActivity
    private var currentErrands: List<Errand> = emptyList()
    private val chatListeners = mutableMapOf<String, ListenerRegistration>()  // Track chat listeners
    private val unreadCounts = mutableMapOf<String, Int>()  // Track unread counts by errand ID

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the RecyclerView
        setupRecyclerView()

        // Set up the Create Job button
        binding.createJobButton.setOnClickListener {
            openCreateJobForm()
        }

        // Listen for user's created errands
        listenForMyErrands()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the listener to update unread counts
        errandsListener?.remove()
        listenForMyErrands()
    }

    private fun setupRecyclerView() {
        // [CHANGE] Pass viewLifecycleOwner to the adapter so it can load offer data
        jobAdapter = JobAdapter(emptyList(), viewLifecycleOwner)

        jobAdapter.isRequesterMode = true // Enable offers dropdown for this screen

        // Handle when requester accepts a runner offer
        jobAdapter.onAcceptRunnerListener = { job, runnerRef ->
            lifecycleScope.launch {
                try {
                    // Assign the runner to the errand in Firestore
                    errandRepository.claimErrand(job.id, runnerRef.id)
                    Toast.makeText(requireContext(), "Runner accepted!", Toast.LENGTH_SHORT).show()
                    // The live listener will automatically update the UI to show it as claimed
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Failed to accept runner: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // disable profile button visibility for requests without runner
        // show claimed badge for requests that's claimed

        // Override the click to open "Edit Job" instead of "View Job"
        jobAdapter.onJobClickListener = { job ->
            // Find the full Errand object by matching the ID
            val errand = currentErrands.find { it.id == job.id }

            if (errand != null) {
                // Open EditJobActivity and pass the full Errand data
                val intent = Intent(requireContext(), EditJobActivity::class.java)

                // Pass all the errand fields as extras
                intent.putExtra("ERRAND_ID", errand.id)
                intent.putExtra("ERRAND_TITLE", errand.title)
                intent.putExtra("ERRAND_DESCRIPTION", errand.description)
                intent.putExtra("ERRAND_CAMPUS", errand.campus)
                intent.putExtra("ERRAND_PRICE", errand.priceOffered)
                intent.putExtra("ERRAND_LOCATION", errand.location)
                intent.putExtra("ERRAND_STATUS", errand.status)
                intent.putExtra("ERRAND_RUNNER_COMPLETION", errand.runnerCompletion)
                intent.putExtra("ERRAND_CLIENT_COMPLETION", errand.clientCompletion)

                // Pass photo URLs as ArrayList (Parcelable)
                intent.putStringArrayListExtra("ERRAND_PHOTO_URLS", ArrayList(errand.photoUrls))

                // Check if errand is claimed (runnerId is not null)
                val isClaimed = errand.runnerId != null
                intent.putExtra("ERRAND_IS_CLAIMED", isClaimed)

                startActivity(intent)
            }
        }

        jobAdapter.onChatClickListener = { job ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra(ChatActivity.EXTRA_ERRAND_ID, job.id)
                putExtra(ChatActivity.EXTRA_ERRAND_TITLE, job.title)
            }
            startActivity(intent)
        }

        jobAdapter.onProfileClickListener = { job ->
            val runnerRef = job.runner

            if (runnerRef != null) {
                val intent = Intent(requireContext(), ProfileDisplayActivity::class.java).apply {
                    putExtra("PERSON_PATH", runnerRef.path)
                    putExtra("ROLE", "runner")
                }
                startActivity(intent)
            } else {
                Log.e("RequestsFragment", "Cannot open profile: Runner ref is null")
            }
        }

        binding.requestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = jobAdapter
        }
    }

    private fun listenForMyErrands() {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val myRef = db.collection("users").document(currentUid)
        val chatRepo = ChatRepository()

        errandsListener = errandRepository.listenErrands(
            query = ErrandQuery(
                requesterId = myRef,
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
                location = errand.location?:"N/A",
                payment = errand.priceOffered?.let { "$$it" } ?: "Free",
                mediaPaths = errand.photoUrls,
                isClaimed = errand.status == "claimed" || errand.runnerId != null,
                requester = errand.requesterId,
                runner = errand.runnerId,
                unreadMessageCount = unreadCounts[errand.id] ?: 0,
                offers = errand.offers
            )
        }
        jobAdapter.submitList(jobs)
    }

    private fun openCreateJobForm() {
        val intent = Intent(requireContext(), CreateJobActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errandsListener?.remove()
        chatListeners.values.forEach { it.remove() }
        chatListeners.clear()
        _binding = null
    }
}