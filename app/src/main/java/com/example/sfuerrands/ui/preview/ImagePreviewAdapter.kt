package com.example.sfuerrands.ui.preview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sfuerrands.databinding.ItemImagePreviewPageBinding

class ImagePreviewAdapter(
    private val urls: List<String>
) : RecyclerView.Adapter<ImagePreviewAdapter.VH>() {

    class VH(val binding: ItemImagePreviewPageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemImagePreviewPageBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val url = urls[position]

        Glide.with(holder.binding.previewImage)
            .load(url)
            .placeholder(android.R.color.darker_gray)
            .fitCenter()
            .into(holder.binding.previewImage)
    }

    override fun getItemCount(): Int = urls.size
}
