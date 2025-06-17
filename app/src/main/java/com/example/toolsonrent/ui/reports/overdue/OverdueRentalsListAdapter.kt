package com.example.toolsonrent.ui.reports.overdue

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.databinding.ItemOverdueRentalBinding // Generated
// OverdueRentalInfo data class is in the same package.
// java.util.Date is used by OverdueRentalInfo.
import java.text.SimpleDateFormat
import java.util.Locale

class OverdueRentalsListAdapter : ListAdapter<OverdueRentalInfo, OverdueRentalsListAdapter.OverdueRentalViewHolder>(OverdueRentalDiffCallback) {

    // Date formatter for displaying dates consistently.
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverdueRentalViewHolder {
        val binding = ItemOverdueRentalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OverdueRentalViewHolder(binding, dateFormat) // Pass dateFormat to ViewHolder
    }

    override fun onBindViewHolder(holder: OverdueRentalViewHolder, position: Int) {
        val overdueRental = getItem(position)
        holder.bind(overdueRental)
    }

    // ViewHolder holds references to the views for each item.
    inner class OverdueRentalViewHolder(
        private val binding: ItemOverdueRentalBinding,
        private val dateFormat: SimpleDateFormat // Store dateFormat in ViewHolder
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(overdueRentalInfo: OverdueRentalInfo) {
            binding.textViewToolNameOverdueItem.text = overdueRentalInfo.toolName
            binding.textViewCustomerNameOverdueItem.text = "Rented by: ${overdueRentalInfo.customerName}"
            binding.textViewDueDateOverdueItem.text = "Was Due: ${dateFormat.format(overdueRentalInfo.dueDate)}"

            val daysOverdueText = if (overdueRentalInfo.daysOverdue == 1L) {
                "1 Day Overdue" // Singular form
            } else {
                "${overdueRentalInfo.daysOverdue} Days Overdue" // Plural form
            }
            binding.textViewDaysOverdueItem.text = daysOverdueText

            // Note: The text color for textViewDaysOverdueItem is set in item_overdue_rental.xml
            // using android:textColor="@android:color/holo_red_dark".
            // It could also be programmatically set here if dynamic conditions were needed.
        }
    }

    // DiffCallback efficiently calculates differences between lists for smooth RecyclerView updates.
    companion object OverdueRentalDiffCallback : DiffUtil.ItemCallback<OverdueRentalInfo>() {
        override fun areItemsTheSame(oldItem: OverdueRentalInfo, newItem: OverdueRentalInfo): Boolean {
            // Check if items represent the same entity (e.g., by a unique ID).
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: OverdueRentalInfo, newItem: OverdueRentalInfo): Boolean {
            // Check if the content of the items is the same.
            // This relies on OverdueRentalInfo being a data class, which implements equals() correctly.
            return oldItem == newItem
        }
    }
}
