package com.example.toolsonrent.ui.toollistscreen

import android.graphics.Color // For default text color
import android.util.TypedValue // For resolving theme attributes
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R
// import com.example.toolsonrent.database.entity.Tool // No longer directly using Tool here
import com.example.toolsonrent.databinding.ItemToolBinding
import java.text.NumberFormat
import java.util.Locale

class ToolListAdapter(
    private val onToolClicked: (ToolWithInstanceCounts) -> Unit // Changed to ToolWithInstanceCounts
) : ListAdapter<ToolWithInstanceCounts, ToolListAdapter.ToolViewHolder>(ToolWithInstanceCountsDiffCallback) { // Changed

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolViewHolder(binding, onToolClicked)
    }

    override fun onBindViewHolder(holder: ToolViewHolder, position: Int) {
        val toolWithCounts = getItem(position) // Changed
        holder.bind(toolWithCounts) // Changed
    }

    inner class ToolViewHolder(
        private val binding: ItemToolBinding,
        private val onToolClickedCallback: (ToolWithInstanceCounts) -> Unit // Changed
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentToolWithCounts: ToolWithInstanceCounts? = null // Changed

        init {
            itemView.setOnClickListener {
                currentToolWithCounts?.let { item -> // Changed
                    onToolClickedCallback(item)
                }
            }
        }

        fun bind(toolWithCounts: ToolWithInstanceCounts) { // Changed
            currentToolWithCounts = toolWithCounts // Changed
            val tool = toolWithCounts.tool // Get the actual Tool entity

            binding.textViewToolNameItem.text = tool.name

            val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            format.maximumFractionDigits = 2
            binding.textViewRentalPriceItem.text = itemView.context.getString(R.string.price_per_day_format, format.format(tool.rentalPrice))


            binding.textViewToolQuantityStatusItem.text =
                itemView.context.getString(R.string.quantity_status_format, toolWithCounts.availableInstanceCount, toolWithCounts.totalInstanceCount)

            if (toolWithCounts.availableInstanceCount <= 0) {
                binding.textViewToolQuantityStatusItem.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_rented_red)
                )
            } else {
                binding.textViewToolQuantityStatusItem.setTextColor(
                     ContextCompat.getColor(itemView.context, R.color.status_available_green)
                )
            }

            if (tool.imageUri != null) {
                Glide.with(itemView.context)
                    .load(tool.imageUri) // Assuming imageUri is a String path/URL
                    .placeholder(R.drawable.ic_image_placeholder) // Use consistent placeholder
                    .error(R.drawable.ic_broken_image) // Use consistent error drawable
                    .into(binding.imageViewToolItem)
            } else {
                // Use consistent placeholder if no image URI
                Glide.with(itemView.context)
                    .load(R.drawable.ic_image_placeholder)
                    .into(binding.imageViewToolItem)
            }
        }
    }

    companion object ToolWithInstanceCountsDiffCallback : DiffUtil.ItemCallback<ToolWithInstanceCounts>() { // Changed
        override fun areItemsTheSame(oldItem: ToolWithInstanceCounts, newItem: ToolWithInstanceCounts): Boolean {
            return oldItem.tool.id == newItem.tool.id // Compare by tool ID
        }

        override fun areContentsTheSame(oldItem: ToolWithInstanceCounts, newItem: ToolWithInstanceCounts): Boolean {
            return oldItem == newItem // Compare full object
        }
    }
}
// Need to add/update string resources:
// <string name="price_per_day_format">%1$s / day</string>
// <string name="quantity_status_format">Available: %1$d / %2$d</string>
// Used R.drawable.ic_image_placeholder and R.drawable.ic_broken_image for consistency.
