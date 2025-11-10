package com.example.sfuerrands.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.data.models.Errand
import com.example.sfuerrands.databinding.FragmentHomeBinding
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private lateinit var jobAdapter: JobAdapter
    private var errandsListener: ListenerRegistration? = null

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
        // Show loading state if you have one
        // binding.progressBar.isVisible = true

        errandsListener = db.collection("errands")
            .whereEqualTo("status", "open")
            // optionally filter by campus:
            // .whereEqualTo("campus", "burnaby")
            .orderBy("createdAt") // or createdAt desc (needs composite index)
            .addSnapshotListener { snapshot, error ->
                // binding.progressBar.isVisible = false

                if (error != null || snapshot == null) {
                    Log.e("HomeFragment", "Firestore error", error)
                    return@addSnapshotListener
                }

                val errands = snapshot.toObjects(Errand::class.java)

                Log.d("HomeFragment", "Got ${errands.size} errands")

                // Map Errand → Job (UI model)
                val jobs = errands.map { errand ->
                    Job(
                        id = errand.id,
                        title = errand.title,
                        description = errand.description,
                        location = errand.campus, // or some location string
                        payment = errand.priceOffered?.let { "$${"%.2f".format(it)}" } ?: "TBD"
                    )
                }

                jobAdapter.submitList(jobs)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        errandsListener?.remove()
        _binding = null
    }
}