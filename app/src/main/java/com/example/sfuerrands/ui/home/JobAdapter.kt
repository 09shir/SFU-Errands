package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.sfuerrands.R
import com.example.sfuerrands.data.models.User
import com.google.firebase.firestore.DocumentReference
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// [CHANGE] Added lifecycleOwner to constructor so we can safely launch coroutines
class JobAdapter(
    private var jobs: List<Job>,
    private val lifecycleOwner: LifecycleOwner? = null
) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    var onJobClickListener: ((Job) -> Unit)? = null
    var onChatClickListener: ((Job) -> Unit)? = null
    var onProfileClickListener: ((Job) -> Unit)? = null

    var onAcceptRunnerListener: ((Job, DocumentReference) -> Unit)? = null

    var showClaimedBadge: Boolean = false
    var isRequesterMode: Boolean = false

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val campusTextView: TextView = itemView.findViewById(R.id.jobCampus)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
        val claimedBadge: TextView = itemView.findViewById(R.id.claimedBadge)
        val chatButton: ImageButton = itemView.findViewById(R.id.jobChatButton)
        val profileButton: ImageButton = itemView.findViewById(R.id.runnerProfileButton)
        val unreadBadge: TextView = itemView.findViewById(R.id.chatUnreadBadge)

        val offerLayout: LinearLayout = itemView.findViewById(R.id.offerSelectionLayout)
        val offerSpinner: Spinner = itemView.findViewById(R.id.offerSpinner)
        val acceptOfferButton: Button = itemView.findViewById(R.id.acceptOfferButton)
        val completedBadge: TextView = itemView.findViewById(R.id.completedBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]

        holder.titleTextView.text = job.title
        holder.descriptionTextView.text = job.description
        holder.campusTextView.text = job.campus
        holder.paymentTextView.text = job.payment

        // Completed badge (visible when job.isCompleted is true)
        if (job.isCompleted) {
            holder.completedBadge.visibility = View.VISIBLE
        } else {
            holder.completedBadge.visibility = View.GONE
        }

        // disable profile button visibility for requests without runner
        if (isRequesterMode && job.runner == null) {
            holder.profileButton.visibility = View.GONE
        } else {
            holder.profileButton.visibility = View.VISIBLE
            holder.profileButton.setOnClickListener {
                onProfileClickListener?.invoke(job)
            }
        }

        if (isRequesterMode && job.isClaimed && !job.isCompleted) {
            holder.claimedBadge.visibility = View.VISIBLE
        } else {
            holder.claimedBadge.visibility = View.GONE
        }

        if (job.isClaimed) {
            holder.chatButton.visibility = View.VISIBLE
            holder.chatButton.setOnClickListener {
                onChatClickListener?.invoke(job)
            }

            // Show unread badge if there are unread messages
            if (job.unreadMessageCount > 0) {
                holder.unreadBadge.visibility = View.VISIBLE
                holder.unreadBadge.text = if (job.unreadMessageCount > 99) "99+" else job.unreadMessageCount.toString()
            } else {
                holder.unreadBadge.visibility = View.GONE
            }
        } else {
            holder.chatButton.visibility = View.GONE
            holder.chatButton.setOnClickListener(null)
            holder.unreadBadge.visibility = View.GONE
        }

        // --- OFFER SYSTEM LOGIC ---

        // 1. Reset state
        holder.offerLayout.visibility = View.GONE
        holder.acceptOfferButton.setOnClickListener(null)

        // 2. Check conditions
        if (isRequesterMode && !job.isClaimed && job.offers.isNotEmpty() && lifecycleOwner != null) {

            // 3. Use the passed-in lifecycleOwner to launch the coroutine safely
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val runnerDisplayNames = mutableListOf<String>()
                    val runnerRefs = mutableListOf<DocumentReference>()

                    for (ref in job.offers) {
                        val snapshot = ref.get().await()
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            val rating = if (user.runnerRatingCount > 0)
                                String.format("%.1f", user.runnerRatingSum / user.runnerRatingCount)
                            else "N/A"

                            runnerDisplayNames.add("${user.displayName} (★$rating)")
                            runnerRefs.add(ref)
                        }
                    }

                    if (runnerDisplayNames.isNotEmpty()) {
                        // Data loaded successfully -> Show UI
                        holder.offerLayout.visibility = View.VISIBLE

                        val adapter = ArrayAdapter(
                            holder.itemView.context,
                            android.R.layout.simple_spinner_item,
                            runnerDisplayNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        holder.offerSpinner.adapter = adapter

                        holder.acceptOfferButton.setOnClickListener {
                            val selectedPos = holder.offerSpinner.selectedItemPosition
                            if (selectedPos != -1) {
                                val selectedRunnerRef = runnerRefs[selectedPos]
                                onAcceptRunnerListener?.invoke(job, selectedRunnerRef)
                            } else {
                                Toast.makeText(holder.itemView.context, "Please select a runner", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    holder.offerLayout.visibility = View.GONE
                }
            }
        }

        // Main Item Click
        holder.itemView.setOnClickListener {
            if (onJobClickListener != null) {
                onJobClickListener?.invoke(job)
            } else {
                val intent = Intent(holder.itemView.context, JobDetailActivity::class.java)
                intent.putExtra("JOB_ID", job.id)
                intent.putExtra("JOB_TITLE", job.title)
                intent.putExtra("JOB_DESCRIPTION", job.description)
                intent.putExtra("JOB_LOCATION", job.location)
                intent.putExtra("JOB_CAMPUS", job.campus)
                intent.putExtra("JOB_PAYMENT", job.payment)
                intent.putStringArrayListExtra("JOB_MEDIA_PATHS", ArrayList(job.mediaPaths))
                holder.itemView.context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = jobs.size

    fun submitList(newJobs: List<Job>) {
        jobs = newJobs
        notifyDataSetChanged()
    }
}