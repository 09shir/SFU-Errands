package com.example.sfuerrands.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.ErrandQuery
import com.example.sfuerrands.data.repository.ErrandRepository
import com.example.sfuerrands.databinding.FragmentHomeBinding
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private lateinit var jobAdapter: JobAdapter
    private var errandsListener: ListenerRegistration? = null
    private val errandRepository = ErrandRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the RecyclerView
        setupRecyclerView()
        listenForErrands()
        return root
    }

    // Set up the RecyclerView with sample jobs
    private fun setupRecyclerView() {

        // Create the adapter with our sample jobs
        jobAdapter = JobAdapter(emptyList())

        // Set up the RecyclerView
        binding.jobsRecyclerView.apply {
            // LinearLayoutManager makes items appear as vertical list
            layoutManager = LinearLayoutManager(context)
            // Connect adapter to RecyclerView
            this.adapter = jobAdapter
        }
    }

    private fun listenForErrands() {
        // 1. Get current User ID to filter out own posts
        val currentUserId = Firebase.auth.currentUser?.uid

        val query = ErrandQuery(
            status = "open",
            orderByCreatedAtDesc = true,
        )
        errandsListener?.remove()
        errandsListener = errandRepository.listenErrands(
            query = query,
            onSuccess = { errands ->
                Log.d("HomeFragment", "Got ${errands.size} errands")

                // 2. Filter the list: Exclude errands where requesterId is me
                val filteredErrands = errands.filter { errand ->
                    errand.requesterId?.id != currentUserId
                }

                val jobs = filteredErrands.map { e ->
                    Job(
                        id = e.id,
                        title = e.title,
                        description = e.description,
                        location = e.campus,
                        payment = e.priceOffered?.let { "$${"%.2f".format(it)}" } ?: "$0.00",
                        mediaPaths = e.photoUrls
                    )
                }
                jobAdapter.submitList(jobs)
            },
            onError = { e ->
                Log.e("HomeFragment", "Errands listen error", e)
                // TODO: show an empty/error state if you have one
                jobAdapter.submitList(emptyList())
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errandsListener?.remove()
        _binding = null
    }
}