package com.example.sfuerrands.ui.myjobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private fun setupRecyclerView() {
        // Create the adapter with empty list initially
        jobAdapter = JobAdapter(emptyList())

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
        // Get current user UID
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w("RequestsFragment", "User not signed in")
            jobAdapter.submitList(emptyList())
            return
        }

        // Create DocumentReference for current user
        val userRef = db.collection("users").document(currentUserId)

        // Query errands where requesterId == current user
        val query = ErrandQuery(
            requesterId = userRef,
            orderByCreatedAtDesc = true
        )

        errandsListener?.remove()
        errandsListener = errandRepository.listenErrands(
            query = query,
            onSuccess = { errands ->
                Log.d("RequestsFragment", "Got ${errands.size} errands")

                // Store the full Errand objects
                currentErrands = errands

                // Map Errand → Job for display
                val jobs = errands.map { errand ->
                    Job(
                        id = errand.id,
                        title = errand.title,
                        description = errand.description,
                        location = errand.campus.replaceFirstChar { it.uppercase() },
                        payment = errand.priceOffered?.let { "$${"%.2f".format(it)}" } ?: "$0.00",
                        isClaimed = errand.runnerId != null,
                        runner = errand.runnerId
                    )
                }

                // Enable claimed badge display in Requests tab
                jobAdapter.showClaimedBadge = true

                jobAdapter.submitList(jobs)
            },
            onError = { e ->
                Log.e("RequestsFragment", "Errands listen error", e)
                jobAdapter.submitList(emptyList())
            }
        )
    }

    private fun openCreateJobForm() {
        val intent = Intent(requireContext(), CreateJobActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errandsListener?.remove()
        _binding = null
    }
}