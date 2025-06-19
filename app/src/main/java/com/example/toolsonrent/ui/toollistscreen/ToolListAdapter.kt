package com.example.toolsonrent.ui.toollistscreen

// Import for Uri if image loading from URI is implemented, not needed for placeholder
// import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide // Glide import
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For R.string, R.color, R.drawable
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.ItemToolBinding
import java.text.NumberFormat // For currency formatting
import java.util.Locale // For Locale.getDefault()

class ToolListAdapter(
    private val onToolClicked: (Tool) -> Unit // New parameter for click callback
) : ListAdapter<Tool, ToolListAdapter.ToolViewHolder>(ToolDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Pass the onToolClicked lambda to the ViewHolder
        return ToolViewHolder(binding, onToolClicked)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val tool = getItem(position)
        holder.bind(tool)
    }

    inner class ToolViewHolder(
        private val binding: ItemToolBinding,
        private val onToolClickedCallback: (Tool) -> Unit // Received callback
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTool: Tool? = null // To hold the current tool for the click listener

        init {
            itemView.setOnClickListener {
                currentTool?.let { tool ->
                    onToolClickedCallback(tool)
                }
            }
        }

        fun bind(tool: Tool) {
            currentTool = tool // Store tool for the click listener

            binding.textViewToolNameItem.text = tool.name

            // Using NumberFormat for currency, assuming default locale for now
            val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.maximumFractionDigits = 2 // Ensure two decimal places
            binding.textViewRentalPriceItem.text = "${format.format(tool.rentalPrice)} / day"

            if (tool.isAvailable) {
                binding.textViewAvailabilityItem.text = itemView.context.getString(R.string.text_available)
                binding.textViewAvailabilityItem.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_available_green)
                )
            } else {
                binding.textViewAvailabilityItem.text = itemView.context.getString(R.string.text_rented)
                binding.textViewAvailabilityItem.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_rented_red)
                )
            }

            // Placeholder for image loading
            if (tool.imageUri != null) {
                // In a real app, you would use Glide or Picasso here:
                // Glide.with(itemView.context).load(Uri.parse(tool.imageUri)).placeholder(R.drawable.ic_menu_gallery).into(binding.imageViewToolItem)
                // For now, just a different placeholder to indicate a URI is present
            // binding.imageViewToolItem.setImageResource(R.drawable.ic_launcher_background) // Example placeholder
            Glide.with(itemView.context)
                .load(tool.imageUri) // tool.imageUri is the String URI of the internal file
                .placeholder(android.R.drawable.ic_menu_gallery) // Default placeholder while loading
                .error(R.drawable.ic_baseline_broken_image_24) // Use the broken image icon on error
                .into(binding.imageViewToolItem)
            } else {
            binding.imageViewToolItem.setImageResource(android.R.drawable.ic_menu_gallery) // Default if no URI
            }
        }
    }

    companion object ToolDiffCallback : DiffUtil.ItemCallback<Tool>() {
        override fun areItemsTheSame(oldItem: Tool, newItem: Tool): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tool, newItem: Tool): Boolean {
            // This assumes Tool is a data class, so `==` checks for content equality.
            return oldItem == newItem
        }
    }
}
