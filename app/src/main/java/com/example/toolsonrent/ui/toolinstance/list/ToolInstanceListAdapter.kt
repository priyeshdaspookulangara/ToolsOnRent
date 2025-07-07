package com.example.toolsonrent.ui.toolinstance.list

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.toolsonrent.R
import com.example.toolsonrent.database.entity.ToolInstance
import com.example.toolsonrent.databinding.ListItemToolInstanceBinding

class ToolInstanceListAdapter(
    private val onEditClick: (ToolInstance) -> Unit
) : ListAdapter<ToolInstance, ToolInstanceListAdapter.ToolInstanceViewHolder>(ToolInstanceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToolInstanceViewHolder {
        val binding = ListItemToolInstanceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ToolInstanceViewHolder(binding, onEditClick)
    }

    override fun onBindViewHolder(holder: ToolInstanceViewHolder, position: Int) {
        val toolInstance = getItem(position)
        holder.bind(toolInstance)
    }

    class ToolInstanceViewHolder(
        private val binding: ListItemToolInstanceBinding,
        private val onEditClick: (ToolInstance) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(toolInstance: ToolInstance) {
            binding.textViewInstanceSerialNumber.text = if (toolInstance.serialNumber.isNullOrEmpty()) {
                itemView.context.getString(R.string.serial_number_na_placeholder, toolInstance.instanceId)
            } else {
                itemView.context.getString(R.string.serial_number_prefix, toolInstance.serialNumber)
            }
            binding.textViewInstanceStatus.text = itemView.context.getString(R.string.status_prefix, toolInstance.status)

            if (toolInstance.notes.isNullOrBlank()) {
                binding.textViewInstanceNotes.visibility = ViewGroup.GONE
            } else {
                binding.textViewInstanceNotes.visibility = ViewGroup.VISIBLE
                binding.textViewInstanceNotes.text = itemView.context.getString(R.string.notes_prefix, toolInstance.notes)
            }

            if (toolInstance.instanceImageUri != null) {
                Glide.with(itemView.context)
                    .load(Uri.parse(toolInstance.instanceImageUri))
                    .placeholder(R.drawable.ic_image_placeholder) // Add a placeholder drawable
                    .error(R.drawable.ic_broken_image) // Add an error drawable
                    .into(binding.imageViewInstance)
            } else {
                Glide.with(itemView.context)
                    .load(R.drawable.ic_image_placeholder) // Default placeholder
                    .into(binding.imageViewInstance)
            }

            binding.buttonEditInstance.setOnClickListener {
                onEditClick(toolInstance)
            }
        }
    }

    private class ToolInstanceDiffCallback : DiffUtil.ItemCallback<ToolInstance>() {
        override fun areItemsTheSame(oldItem: ToolInstance, newItem: ToolInstance): Boolean {
            return oldItem.instanceId == newItem.instanceId
        }

        override fun areContentsTheSame(oldItem: ToolInstance, newItem: ToolInstance): Boolean {
            return oldItem == newItem
        }
    }
}
// Need to add these to strings.xml:
// <string name="serial_number_prefix">SN: %1$s</string>
// <string name="serial_number_na_placeholder">ID: %1$d (No S/N)</string>
// <string name="status_prefix">Status: %1$s</string>
// <string name="notes_prefix">Notes: %1$s</string>
// Need to add placeholder drawables: ic_image_placeholder, ic_broken_image
// Need to create the package com.example.toolsonrent.ui.toolinstance.list if it doesn't exist.
// The context for strings is itemView.context, which is good.
// Using Glide for image loading is good.
// DiffUtil is correctly implemented.
// Click listener for edit is set up.
