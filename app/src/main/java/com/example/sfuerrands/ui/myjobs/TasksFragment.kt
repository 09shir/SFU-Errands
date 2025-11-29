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
import com.example.sfuerrands.databinding.FragmentTasksBinding
import com.example.sfuerrands.ui.chat.ChatActivity
import com.example.sfuerrands.ui.home.Job
import com.example.sfuerrands.ui.home.JobAdapter
import com.example.sfuerrands.ui.profile.ProfileDisplayActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

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
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Log.w("TasksFragment", "User not signed in")
            jobAdapter.submitList(emptyList())
            return
        }

        // Create DocumentReference for current user
        val userRef = db.collection("users").document(currentUserId)

        // Query errands where runnerId == current user
        val query = ErrandQuery(
            runnerId = userRef,
            orderByCreatedAtDesc = true
        )

        errandsListener?.remove()
        errandsListener = errandRepository.listenErrands(
            query = query,
            onSuccess = { errands ->
                Log.d("TasksFragment", "Got ${errands.size} tasks")
                currentErrands = errands

                // Map Errand → Job for display
                val jobs = errands.map { errand ->
                    Job(
                        id = errand.id,
                        title = errand.title,
                        description = errand.description,
                        location = errand.campus.replaceFirstChar { it.uppercase() },
                        payment = errand.priceOffered?.let { "$${"%.2f".format(it)}" } ?: "$0.00",
                        mediaPaths = errand.photoUrls,
                        isClaimed = true,
                        requester = errand.requesterId
                    )
                }

                jobAdapter.submitList(jobs)
            },
            onError = { e ->
                Log.e("TasksFragment", "Tasks listen error", e)
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