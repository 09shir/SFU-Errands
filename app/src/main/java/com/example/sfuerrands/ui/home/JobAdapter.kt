package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import android.widget.ImageButton
import com.example.sfuerrands.R

class JobAdapter(private var jobs: List<Job>) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {
    var onJobClickListener: ((Job) -> Unit)? = null
    var onChatClickListener: ((Job) -> Unit)? = null
    var onProfileClickListener: ((Job) -> Unit)? = null
    var requestTab: Boolean = false

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val campusTextView: TextView = itemView.findViewById(R.id.jobCampus)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
        val claimedBadge: TextView = itemView.findViewById(R.id.claimedBadge)
        val chatButton: ImageButton = itemView.findViewById(R.id.jobChatButton)
        val profileButton: ImageButton = itemView.findViewById(R.id.runnerProfileButton)
        val unreadBadge: TextView = itemView.findViewById(R.id.chatUnreadBadge)  // NEW
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

        // disable profile button visibility for requests without runner
        if (requestTab && job.runner == null) {
            holder.profileButton.visibility = View.GONE
        } else {
            holder.profileButton.visibility = View.VISIBLE
            holder.profileButton.setOnClickListener {
                onProfileClickListener?.invoke(job)
            }
        }

        if (requestTab && job.isClaimed) {
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
                holder.unreadBadge.text = if (job.unreadMessageCount > 99) {
                    "99+"
                } else {
                    job.unreadMessageCount.toString()
                }
            } else {
                holder.unreadBadge.visibility = View.GONE
            }
        } else {
            holder.chatButton.visibility = View.GONE
            holder.chatButton.setOnClickListener(null)
            holder.unreadBadge.visibility = View.GONE
        }

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