package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.databinding.FragmentRequestsBinding
import com.example.sfuerrands.ui.home.Job
import com.example.sfuerrands.ui.home.JobAdapter

// Fragment for the users requests in myJobs
class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!

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

        // Set up the RecyclerView with sample jobs YOU posted
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // hardcoded sample jobs that YOU posted
        // TODO: these will come from a database later
        val myPostedJobs = listOf(
            Job(
                title = "Pick up coffee from Tim Hortons",
                description = "Need someone to grab me a medium coffee. Will meet you at the library.",
                location = "AQ Building",
                payment = "$5.00"
            ),
            Job(
                title = "Print assignment",
                description = "Need 10 pages printed. I'll send you the PDF.",
                location = "Library",
                payment = "$4.00"
            )
        )

        val adapter = JobAdapter(myPostedJobs)

        binding.requestsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}