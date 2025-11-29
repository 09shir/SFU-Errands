package com.example.sfuerrands.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.sfuerrands.R
import com.example.sfuerrands.data.models.ChatMessage
import com.google.firebase.firestore.DocumentReference

class ChatAdapter(
    private val myUid: String?
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    private val TYPE_ME = 1
    private val TYPE_THEM = 2

    fun submit(list: List<ChatMessage>) {
        messages.clear()
        messages.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        val senderRef: DocumentReference? = msg.senderId
        val senderUid = senderRef?.id
        return if (senderUid == myUid) TYPE_ME else TYPE_THEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ME) {
            val v = inflater.inflate(R.layout.item_chat_message_me, parent, false)
            MeVH(v)
        } else {
            val v = inflater.inflate(R.layout.item_chat_message_them, parent, false)
            ThemVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is MeVH) holder.bind(msg) else if (holder is ThemVH) holder.bind(msg)
    }

    override fun getItemCount(): Int = messages.size

    // --- ViewHolders ---

    class MeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        private val imageView: ImageView = itemView.findViewById(R.id.messageImage)
        private val timeView: TextView = itemView.findViewById(R.id.messageTime)
        private val statusView: TextView = itemView.findViewById(R.id.messageStatus)
        private val timeFormatter =
            java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())

        fun bind(msg: ChatMessage) {
            bindCommon(msg, textView, imageView)

            timeView.text = msg.createdAt?.toDate()?.let { date ->
                timeFormatter.format(date)
            } ?: ""

            statusView.visibility = if (msg.read) View.VISIBLE else View.GONE
        }
    }

    class ThemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.messageText)
        private val imageView: ImageView = itemView.findViewById(R.id.messageImage)
        private val timeView: TextView = itemView.findViewById(R.id.messageTime)
        private val timeFormatter =
            java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())

        fun bind(msg: ChatMessage) {
            bindCommon(msg, textView, imageView)

            timeView.text = msg.createdAt?.toDate()?.let { date ->
                timeFormatter.format(date)
            } ?: ""
        }
    }

    companion object {
        private fun bindCommon(
            msg: ChatMessage,
            textView: TextView,
            imageView: ImageView
        ) {
            // Text
            if (!msg.text.isNullOrBlank()) {
                textView.visibility = View.VISIBLE
                textView.text = msg.text
            } else {
                textView.visibility = View.GONE
            }

            // Only show the first image for now
            val firstMedia = msg.media?.firstOrNull()
            if (firstMedia != null) {
                imageView.visibility = View.VISIBLE
                Glide.with(imageView)
                    .load(firstMedia)
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE
            }
        }
    }
}
