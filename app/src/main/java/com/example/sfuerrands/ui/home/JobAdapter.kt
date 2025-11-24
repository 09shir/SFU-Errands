package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.sfuerrands.R

class JobAdapter(private var jobs: List<Job>) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    var onJobClickListener: ((Job) -> Unit)? = null
    var showClaimedBadge: Boolean = false  // NEW: Control whether to show claimed badge

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val locationTextView: TextView = itemView.findViewById(R.id.jobLocation)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
        val claimedBadge: TextView = itemView.findViewById(R.id.claimedBadge)  // NEW
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
        holder.locationTextView.text = job.location
        holder.paymentTextView.text = job.payment

        // NEW: Show/hide claimed badge based on isClaimed property
        if (showClaimedBadge && job.isClaimed) {
            holder.claimedBadge.visibility = View.VISIBLE
        } else {
            holder.claimedBadge.visibility = View.GONE
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