package com.example.toolsonrent.ui.reports.toolhistory

import android.graphics.Color // For default text color
import android.util.TypedValue // For resolving theme attributes
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.ItemToolBinding
import java.text.NumberFormat
import java.util.Locale

class SelectToolAdapter(
    private val onToolSelected: (Tool) -> Unit
) : ListAdapter<Tool, SelectToolAdapter.SelectToolViewHolder>(SelectToolDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectToolViewHolder {
        val binding = ItemToolBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SelectToolViewHolder(binding, onToolSelected)
    }

    override fun onBindViewHolder(holder: SelectToolViewHolder, position: Int) {
        val tool = getItem(position)
        holder.bind(tool)
    }

    inner class SelectToolViewHolder(
        private val binding: ItemToolBinding,
        private val onToolSelectedCallback: (Tool) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentTool: Tool? = null

        init {
            itemView.setOnClickListener {
                currentTool?.let { tool ->
                    onToolSelectedCallback(tool)
                }
            }
        }

        fun bind(tool: Tool) {
            this.currentTool = tool

            binding.textViewToolNameItem.text = tool.name

            val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
            currencyFormat.maximumFractionDigits = 2
            binding.textViewRentalPriceItem.text = "${currencyFormat.format(tool.rentalPrice)} / day"

            // New quantity status logic:
            binding.textViewToolQuantityStatusItem.text =
                "Available: ${tool.currentAvailableQuantity} / ${tool.totalQuantity}"

            if (tool.currentAvailableQuantity <= 0) {
                binding.textViewToolQuantityStatusItem.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.status_rented_red) // Red for out of stock
                )
            } else {
                // Use a less prominent color for normal availability
                binding.textViewToolQuantityStatusItem.setTextColor(
                     ContextCompat.getColor(itemView.context, R.color.status_available_green)
                )
                // // Alternative: Use theme's default secondary text color
                // val typedValue = TypedValue()
                // itemView.context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
                // binding.textViewToolQuantityStatusItem.setTextColor(typedValue.data)
            }

            // Image loading logic (already updated to Glide)
            if (tool.imageUri != null) {
                Glide.with(itemView.context)
                    .load(tool.imageUri)
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
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tool, newItem: Tool): Boolean {
            return oldItem == newItem
        }
    }
}
