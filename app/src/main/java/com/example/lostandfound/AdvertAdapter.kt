package com.example.lostandfound

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lostandfound.databinding.ItemAdvertBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdvertAdapter(
    private val onItemClick: (AdvertItem) -> Unit
) : RecyclerView.Adapter<AdvertAdapter.ViewHolder>() {

    private var items: List<AdvertItem> = emptyList()

    fun submitList(newItems: List<AdvertItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemAdvertBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdvertItem) {
            binding.tvPostType.text = item.postType.uppercase()
            binding.tvCategory.text = item.category
            binding.tvDescription.text = item.description.take(80)
            binding.tvLocation.text = item.location
            binding.tvTimestamp.text = getRelativeTime(item.timestamp)

            // Color for Lost vs Found
            val color = if (item.postType == "Lost") {
                binding.root.context.getColor(R.color.color_lost)
            } else {
                binding.root.context.getColor(R.color.color_found)
            }
            binding.tvPostType.setTextColor(color)

            // Thumbnail
            if (item.imagePath != null) {
                try {
                    binding.imgThumb.setImageURI(Uri.parse(item.imagePath))
                } catch (_: Exception) {
                    binding.imgThumb.setImageResource(R.drawable.ic_image_placeholder)
                }
            } else {
                binding.imgThumb.setImageResource(R.drawable.ic_image_placeholder)
            }

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdvertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    private fun getRelativeTime(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours   = TimeUnit.MILLISECONDS.toHours(diff)
        val days    = TimeUnit.MILLISECONDS.toDays(diff)
        return when {
            seconds < 60   -> "Just now"
            minutes < 60   -> "${minutes} min ago"
            hours < 24     -> "${hours} hr ago"
            days == 1L     -> "Yesterday"
            else           -> "${days} days ago"
        }
    }
}
