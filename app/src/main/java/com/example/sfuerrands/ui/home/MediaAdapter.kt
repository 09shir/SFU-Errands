package com.example.sfuerrands.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sfuerrands.R

class MediaAdapter(
    private var urls: List<String>,
    private val onClick: (position: Int, url: String) -> Unit
) : RecyclerView.Adapter<MediaAdapter.VH>() {

    class VH(val iv: ImageView) : RecyclerView.ViewHolder(iv)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media_image, parent, false) as ImageView
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {

        Glide.with(holder.iv)
            .load(urls[position])
            .into(holder.iv)
        holder.iv.setOnClickListener { onClick(position, urls[position]) }
    }

    override fun getItemCount() = urls.size

    fun submit(list: List<String>) {
        urls = list
        notifyDataSetChanged()
    }
}
