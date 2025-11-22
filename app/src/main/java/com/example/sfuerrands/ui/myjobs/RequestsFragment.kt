package com.example.sfuerrands.ui.myjobs

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.ErrandQuery
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.databinding.FragmentRequestsBinding
import com.example.sfuerrands.ui.home.Job
import com.example.sfuerrands.ui.home.JobAdapter
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
            val intent = Intent(requireContext(), EditJobActivity::class.java)

            // Pass the existing data to the edit screen
            intent.putExtra("JOB_TITLE", job.title)
            intent.putExtra("JOB_DESCRIPTION", job.description)
            intent.putExtra("JOB_LOCATION", job.location)
            intent.putExtra("JOB_PAYMENT", job.payment)

            startActivity(intent)
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

                // Map Errand → Job
                val jobs = errands.map { errand ->
                    Job(
                        id = errand.id,
                        title = errand.title,
                        description = errand.description,
                        location = errand.campus.replaceFirstChar { it.uppercase() }, // "burnaby" → "Burnaby"
                        payment = errand.priceOffered?.let { "$${"%.2f".format(it)}" } ?: "$0.00"
                    )
                }

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