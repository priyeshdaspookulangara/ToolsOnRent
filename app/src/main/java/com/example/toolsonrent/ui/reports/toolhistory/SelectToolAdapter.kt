package com.example.toolsonrent.ui.reports.toolhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Glide import
import com.example.toolsonrent.R // For R.string and R.color resources
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.ItemToolBinding // Reusing the item binding from general tool list
import java.text.NumberFormat // For currency formatting
import java.util.Locale

class SelectToolAdapter(
    private val onToolSelected: (Tool) -> Unit
) : ListAdapter<Tool, SelectToolAdapter.SelectToolViewHolder>(SelectToolDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Pass the onToolSelected lambda to the ViewHolder
        return SelectToolViewHolder(binding, onToolSelected)
    }

    override fun onBindViewHolder(holder: SelectToolViewHolder, position: Int) {
        val tool = getItem(position)
        holder.bind(tool)
    }

    inner class SelectToolViewHolder(
        private val binding: ItemToolBinding,
        private val onToolSelectedCallback: (Tool) -> Unit // Callback to be invoked on item click
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTool: Tool? = null

        init {
            // Set the click listener on the itemView.
            // This listener will use the `currentTool` that is set in the `bind` method.
            itemView.setOnClickListener {
                currentTool?.let { tool ->
                    onToolSelectedCallback(tool)
                }
            }
        }

        fun bind(tool: Tool) {
            this.currentTool = tool // Update the current tool reference

            binding.textViewToolNameItem.text = tool.name

            // Format currency using NumberFormat for locale-awareness
            val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            currencyFormat.maximumFractionDigits = 2 // Ensure two decimal places
            binding.textViewRentalPriceItem.text = "${currencyFormat.format(tool.rentalPrice)} / day"

            // Set availability status and text color
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

            // Handle tool image placeholder
            // In a real application, you would use an image loading library like Glide or Picasso here.
            // e.g., Glide.with(itemView.context).load(tool.imageUri ?: R.drawable.ic_default_tool_placeholder).into(binding.imageViewToolItem)
            if (tool.imageUri != null) {
                Glide.with(itemView.context)
                    .load(tool.imageUri) // tool.imageUri is the String URI of the internal file
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(R.drawable.ic_baseline_broken_image_24)
                    .into(binding.imageViewToolItem)
            } else {
                binding.imageViewToolItem.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    companion object SelectToolDiffCallback : DiffUtil.ItemCallback<Tool>() {
        override fun areItemsTheSame(oldItem: Tool, newItem: Tool): Boolean {
            // Efficiently check if items are the same entity by comparing their unique IDs.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tool, newItem: Tool): Boolean {
            // Check if the content of the items is the same.
            // This relies on Tool being a data class, which implements equals() correctly.
            return oldItem == newItem
        }
    }
}
