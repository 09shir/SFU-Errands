package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.sfuerrands.R


private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_JOB = 1

class JobAdapter(private var items: List<Any>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var onJobClickListener: ((Job) -> Unit)? = null
    var showClaimedBadge: Boolean = false

    // Header ViewHolder
    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val headerText: TextView = itemView.findViewById(R.id.sectionHeaderText)
    }

    // Existing JobViewHolder (kept almost same)
    class JobViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.jobTitle)
        val descriptionTextView: TextView = itemView.findViewById(R.id.jobDescription)
        val locationTextView: TextView = itemView.findViewById(R.id.jobLocation)
        val paymentTextView: TextView = itemView.findViewById(R.id.jobPayment)
        val claimedBadge: TextView = itemView.findViewById(R.id.claimedBadge)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is String -> VIEW_TYPE_HEADER
            is Job -> VIEW_TYPE_JOB
            else -> VIEW_TYPE_JOB
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.section_header, parent, false)
            return HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_job, parent, false)
            return JobViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is HeaderViewHolder) {
            holder.headerText.text = item as String
        } else if (holder is JobViewHolder) {
            val job = item as Job
            holder.titleTextView.text = job.title
            holder.descriptionTextView.text = job.description
            holder.locationTextView.text = job.location
            holder.paymentTextView.text = job.payment

            if (showClaimedBadge && job.isClaimed) {
                holder.claimedBadge.visibility = View.VISIBLE
            } else {
                holder.claimedBadge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                onJobClickListener?.invoke(job) ?: run {
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
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newItems: List<Any>) {
        items = newItems
        notifyDataSetChanged()
    }
}