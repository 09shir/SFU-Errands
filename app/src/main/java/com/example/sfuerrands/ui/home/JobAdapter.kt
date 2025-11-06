package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent


import com.example.sfuerrands.R

// This adapter connects our list of jobs to the RecyclerView
class JobAdapter(private val jobs: List<Job>) : RecyclerView.Adapter<JobAdapter.JobViewHolder>() {

    // ViewHolder holds the views for each job item
    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val locationTextView: TextView = itemView.findViewById(R.id.jobLocation)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
    }

    // Create a new ViewHolder when needed
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view)
    }

    // Puts the data into the ViewHolder
    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = jobs[position]  // Get the job at this position
        
        // Set the text for each TextView
        holder.titleTextView.text = job.title
        holder.descriptionTextView.text = job.description
        holder.locationTextView.text = job.location
        holder.paymentTextView.text = job.payment

        // Make entire card clickable
        holder.itemView.setOnClickListener {
            // Create intent to open JobDetailActivity
            val intent = Intent(holder.itemView.context, JobDetailActivity::class.java)

            // init the intent
            intent.putExtra("JOB_TITLE", job.title)
            intent.putExtra("JOB_DESCRIPTION", job.description)
            intent.putExtra("JOB_LOCATION", job.location)
            intent.putExtra("JOB_PAYMENT", job.payment)

            // Start the detail activity
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = jobs.size
}
