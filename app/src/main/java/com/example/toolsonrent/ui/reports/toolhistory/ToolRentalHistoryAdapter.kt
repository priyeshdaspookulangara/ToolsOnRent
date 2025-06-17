package com.example.toolsonrent.ui.reports.toolhistory

import android.graphics.Color // For default color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For R.color resources
import com.example.toolsonrent.databinding.ItemToolRentalHistoryBinding // Generated
// ToolRentalHistoryItem data class is in the same package.
// java.util.Date is used by ToolRentalHistoryItem.
import java.text.SimpleDateFormat
import java.util.Locale

class ToolRentalHistoryAdapter : ListAdapter<ToolRentalHistoryItem, ToolRentalHistoryAdapter.HistoryViewHolder>(ToolHistoryDiffCallback) {

    // Date formatter for displaying dates consistently.
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemToolRentalHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding, dateFormat) // Pass dateFormat to ViewHolder
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = getItem(position)
        holder.bind(historyItem)
    }

    // ViewHolder holds references to the views for each item.
    inner class HistoryViewHolder(
        private val binding: ItemToolRentalHistoryBinding,
        private val dateFormat: SimpleDateFormat // Store dateFormat in ViewHolder
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ToolRentalHistoryItem) {
            binding.textViewCustomerNameToolHistoryItem.text = "Rented by: ${item.customerName}"
            binding.textViewRentalDateToolHistoryItem.text = "Rented: ${dateFormat.format(item.rentalDate)}"
            binding.textViewDueDateToolHistoryItem.text = "Due: ${dateFormat.format(item.dueDate)}"

            if (item.returnDate != null) {
                binding.textViewReturnDateToolHistoryItem.text = "Returned: ${dateFormat.format(item.returnDate)}"
                binding.textViewReturnDateToolHistoryItem.visibility = View.VISIBLE
            } else {
                binding.textViewReturnDateToolHistoryItem.visibility = View.GONE
            }

            binding.textViewStatusToolHistoryItem.text = item.status
            val statusColor = when (item.status) {
                "Returned" -> ContextCompat.getColor(itemView.context, R.color.status_available_green)
                "Overdue" -> ContextCompat.getColor(itemView.context, R.color.status_rented_red)
                "Active" -> ContextCompat.getColor(itemView.context, R.color.status_active_blue)
                else -> Color.BLACK // Default color if status is unrecognized
            }
            binding.textViewStatusToolHistoryItem.setTextColor(statusColor)
        }
    }

    // DiffCallback efficiently calculates differences between lists for smooth RecyclerView updates.
    companion object ToolHistoryDiffCallback : DiffUtil.ItemCallback<ToolRentalHistoryItem>() {
        override fun areItemsTheSame(oldItem: ToolRentalHistoryItem, newItem: ToolRentalHistoryItem): Boolean {
            // Check if items represent the same entity (e.g., by a unique ID).
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: ToolRentalHistoryItem, newItem: ToolRentalHistoryItem): Boolean {
            // Check if the content of the items is the same.
            // This relies on ToolRentalHistoryItem being a data class, which implements equals() correctly.
            return oldItem == newItem
        }
    }
}
