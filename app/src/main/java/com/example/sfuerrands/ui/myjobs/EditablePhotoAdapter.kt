package com.example.sfuerrands.ui.myjobs

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sfuerrands.R

// Sealed class to represent either a URL (existing photo) or URI (newly added photo)
sealed class PhotoItem {
    data class ExistingPhoto(val url: String, val originalGsUrl: String) : PhotoItem()
    data class NewPhoto(val uri: Uri) : PhotoItem()
}

class EditablePhotoAdapter(
    private var photos: List<PhotoItem>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<EditablePhotoAdapter.VH>() {

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgPhoto)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_photo, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val photo = photos[position]
        
        // Load image using Glide (works with both URLs and URIs)
        when (photo) {
            is PhotoItem.ExistingPhoto -> {
                Glide.with(holder.img)
                    .load(photo.url)
                    .into(holder.img)
            }
            is PhotoItem.NewPhoto -> {
                Glide.with(holder.img)
                    .load(photo.uri)
                    .into(holder.img)
            }
        }

        // Handle remove button click
        holder.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onRemoveClick(pos)
            }
        }
    }

    override fun getItemCount(): Int = photos.size

    fun submit(newPhotos: List<PhotoItem>) {
        photos = newPhotos
        notifyDataSetChanged()
    }
}
