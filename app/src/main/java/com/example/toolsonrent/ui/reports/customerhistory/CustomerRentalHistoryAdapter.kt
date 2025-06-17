package com.example.toolsonrent.ui.reports.customerhistory

import android.graphics.Color // For default color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For colors R.color.status_...
import com.example.toolsonrent.databinding.ItemCustomerRentalHistoryBinding // Generated
// CustomerRentalHistoryItem is in the same package.
// java.util.Date is used by CustomerRentalHistoryItem.
import java.text.SimpleDateFormat
import java.util.Locale

class CustomerRentalHistoryAdapter : ListAdapter<CustomerRentalHistoryItem, CustomerRentalHistoryAdapter.HistoryViewHolder>(CustomerHistoryDiffCallback) {

    // Date formatter for displaying dates consistently.
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemCustomerRentalHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding, dateFormat) // Pass dateFormat to ViewHolder
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = getItem(position)
        holder.bind(historyItem)
    }

    // ViewHolder holds references to the views for each item.
    inner class HistoryViewHolder(
        private val binding: ItemCustomerRentalHistoryBinding,
        private val dateFormat: SimpleDateFormat // Store dateFormat in ViewHolder
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CustomerRentalHistoryItem) {
            binding.textViewToolNameHistoryItem.text = item.toolName
            binding.textViewRentalDateHistoryItem.text = "Rented: ${dateFormat.format(item.rentalDate)}"
            binding.textViewDueDateHistoryItem.text = "Due: ${dateFormat.format(item.dueDate)}"

            if (item.returnDate != null) {
                binding.textViewReturnDateHistoryItem.text = "Returned: ${dateFormat.format(item.returnDate)}"
                binding.textViewReturnDateHistoryItem.visibility = View.VISIBLE
            } else {
                binding.textViewReturnDateHistoryItem.visibility = View.GONE
            }

            binding.textViewStatusHistoryItem.text = item.status
            val statusColor = when (item.status) {
                "Returned" -> ContextCompat.getColor(itemView.context, R.color.status_available_green)
                "Overdue" -> ContextCompat.getColor(itemView.context, R.color.status_rented_red)
                "Active" -> ContextCompat.getColor(itemView.context, R.color.status_active_blue)
                else -> Color.BLACK // Default color if status is unrecognized
            }
            binding.textViewStatusHistoryItem.setTextColor(statusColor)
        }
    }

    // DiffCallback efficiently calculates differences between lists for smooth RecyclerView updates.
    companion object CustomerHistoryDiffCallback : DiffUtil.ItemCallback<CustomerRentalHistoryItem>() {
        override fun areItemsTheSame(oldItem: CustomerRentalHistoryItem, newItem: CustomerRentalHistoryItem): Boolean {
            // Check if items represent the same entity (e.g., by a unique ID).
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: CustomerRentalHistoryItem, newItem: CustomerRentalHistoryItem): Boolean {
            // Check if the content of the items is the same.
            // This relies on CustomerRentalHistoryItem being a data class, which implements equals() correctly.
            return oldItem == newItem
        }
    }
}
