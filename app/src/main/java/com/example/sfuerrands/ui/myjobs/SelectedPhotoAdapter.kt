package com.example.sfuerrands.ui.myjobs

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sfuerrands.R

class SelectedPhotoAdapter(
    private var uris: List<Uri>,
    private val onRemoveClick: (position: Int) -> Unit
) : RecyclerView.Adapter<SelectedPhotoAdapter.VH>() {

    class VH(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.imgPhoto)
        val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_photo, parent, false)  // <-- important
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img)
            .load(uris[position])
            .into(holder.img)

        holder.btnRemove.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onRemoveClick(pos)
            }
        }
    }

    override fun getItemCount(): Int = uris.size

    fun submit(newUris: List<Uri>) {
        uris = newUris
        notifyDataSetChanged()
    }
}
