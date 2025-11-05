package com.example.sfuerrands.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Set up the RecyclerView
        setupRecyclerView()

        return root
    }

    // Set up the RecyclerView with sample jobs
    private fun setupRecyclerView() {
        // Create a list of sample jobs
        // These would come from a database or server when we build it
        val sampleJobs = listOf(
            Job(
                title = "Pick up coffee from Tim Hortons",
                description = "Need someone to grab me a medium coffee. Will meet you at the library.",
                location = "AQ Building",
                payment = "$5.00"
            ),
            Job(
                title = "Return library books",
                description = "I have 3 books that need to be returned to Bennett Library by 5pm today.",
                location = "Bennett Library",
                payment = "$8.00"
            ),
            Job(
                title = "Deliver notes from CMPT class",
                description = "Missed today's lecture. Need notes delivered to my dorm.",
                location = "TASC Building",
                payment = "$10.00"
            ),
            Job(
                title = "Buy lunch from dining hall",
                description = "Can someone grab me a sandwich and bring it to the study room?",
                location = "Maggie Benston Centre",
                payment = "$7.00"
            ),
            Job(
                title = "Print assignment",
                description = "Need 10 pages printed. I'll send you the PDF.",
                location = "Library",
                payment = "$4.00"
            )
        )

        // Create the adapter with our sample jobs
        val adapter = JobAdapter(sampleJobs)

        // Set up the RecyclerView
        binding.jobsRecyclerView.apply {
            // LinearLayoutManager makes items appear as vertical list
            layoutManager = LinearLayoutManager(context)
            // Connect adapter to RecyclerView
            this.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}