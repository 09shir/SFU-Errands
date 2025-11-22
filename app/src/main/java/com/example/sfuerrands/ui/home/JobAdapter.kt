package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.sfuerrands.R
import com.example.sfuerrands.ui.myjobs.EditJobActivity // Make sure this import is here

class JobAdapter(private var jobs: List<Job>) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    // 1. ADD THIS VARIABLE: A custom click listener (nullable)
    var onJobClickListener: ((Job) -> Unit)? = null

    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val locationTextView: TextView = itemView.findViewById(R.id.jobLocation)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
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

        // 2. UPDATE THE CLICK LISTENER
        holder.itemView.setOnClickListener {

            // Check if a custom listener exists (e.g., for "Requests" tab)
            if (onJobClickListener != null) {
                // Execute the custom action
                onJobClickListener?.invoke(job)
            } else {
                // DEFAULT BEHAVIOR (for "Home" tab): Open View Details
                val intent = Intent(holder.itemView.context, JobDetailActivity::class.java)
                intent.putExtra("JOB_ID", job.id)
                intent.putExtra("JOB_TITLE", job.title)
                intent.putExtra("JOB_DESCRIPTION", job.description)
                intent.putExtra("JOB_LOCATION", job.location)
                intent.putExtra("JOB_PAYMENT", job.payment)
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