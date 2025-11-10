package com.example.sfuerrands.ui.myjobs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sfuerrands.databinding.FragmentTasksBinding
import com.example.sfuerrands.ui.home.Job
import com.example.sfuerrands.ui.home.JobAdapter

// fragment that you're RUNNING for other people
class TasksFragment : Fragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

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
    }

    private fun setupRecyclerView() {
        // hardcoded sample jobs that YOU are running
        // TODO: these will come from a database later
        val myActiveTasks = listOf(
            Job(
                id = "1",
                title = "Return library books",
                description = "I have 3 books that need to be returned to Bennett Library by 5pm today.",
                location = "Bennett Library",
                payment = "$8.00"
            ),
            Job(
                id = "2",
                title = "Buy lunch from dining hall",
                description = "Can someone grab me a sandwich and bring it to the study room?",
                location = "Maggie Benston Centre",
                payment = "$7.00"
            ),
            Job(
                id = "3",
                title = "Deliver notes from CMPT class",
                description = "Missed today's lecture. Need notes delivered to my dorm.",
                location = "TASC Building",
                payment = "$10.00"
            )
        )

        val adapter = JobAdapter(myActiveTasks)

        binding.tasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}