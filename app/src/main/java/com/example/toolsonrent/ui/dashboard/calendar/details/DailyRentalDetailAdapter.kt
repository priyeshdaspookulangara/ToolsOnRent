package com.example.toolsonrent.ui.dashboard.calendar.details

import android.graphics.Color // For default text color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For color resources
import com.example.toolsonrent.databinding.ItemDailyRentalDetailBinding // Generated
// DailyRentalDetailItem data class is in the same package.
// java.util.Date is used by DailyRentalDetailItem but not directly in this adapter.
import java.util.Locale // For Locale.ROOT in toLowerCase

class DailyRentalDetailAdapter : ListAdapter<DailyRentalDetailItem, DailyRentalDetailAdapter.DetailViewHolder>(DailyRentalDetailDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailViewHolder {
        val binding = ItemDailyRentalDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class DetailViewHolder(
        private val binding: ItemDailyRentalDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DailyRentalDetailItem) {
            binding.textViewToolNameDailyDetail.text = item.toolName
            binding.textViewCustomerNameDailyDetail.text = "Rented to: ${item.customerName}" // Added prefix for clarity
            binding.textViewStatusDailyDetail.text = item.status

            // Set status text color based on the status string (determined in DashboardViewModel)
            val statusColor = when (item.status.toLowerCase(Locale.ROOT)) {
                "due today" -> ContextCompat.getColor(itemView.context, R.color.calendar_due_today_yellow)
                "overdue (was due this day)",
                "overdue (due this day)", // Handling variation from VM
                "overdue" -> ContextCompat.getColor(itemView.context, R.color.status_rented_red)
                "upcoming (due this future day)",
                "due this day" -> ContextCompat.getColor(itemView.context, R.color.status_active_blue)
                "returned" -> ContextCompat.getColor(itemView.context, R.color.status_available_green)
                else -> Color.DKGRAY // Default color for any unexpected status string
            }
            binding.textViewStatusDailyDetail.setTextColor(statusColor)
        }
    }

    companion object DailyRentalDetailDiffCallback : DiffUtil.ItemCallback<DailyRentalDetailItem>() {
        override fun areItemsTheSame(oldItem: DailyRentalDetailItem, newItem: DailyRentalDetailItem): Boolean {
            // Assuming transactionId is unique and stable for each item representing a rental event.
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: DailyRentalDetailItem, newItem: DailyRentalDetailItem): Boolean {
            // Relies on DailyRentalDetailItem being a data class, which implements equals() based on all properties.
            return oldItem == newItem
        }
    }
}
