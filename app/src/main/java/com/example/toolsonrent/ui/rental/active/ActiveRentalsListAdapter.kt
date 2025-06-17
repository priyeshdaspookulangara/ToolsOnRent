package com.example.toolsonrent.ui.rental.active

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.databinding.ItemActiveRentalBinding // Generated
// ActiveRentalInfo is in the same package, so no explicit import needed.
// java.util.Date is used by ActiveRentalInfo.
import java.text.SimpleDateFormat
import java.util.Locale

class ActiveRentalsListAdapter(
    private val onItemReturnedClicked: (ActiveRentalInfo) -> Unit
) : ListAdapter<ActiveRentalInfo, ActiveRentalsListAdapter.ActiveRentalViewHolder>(ActiveRentalDiffCallback) {

    // Date formatter for displaying dates consistently.
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActiveRentalViewHolder {
        val binding = ItemActiveRentalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActiveRentalViewHolder(binding, dateFormat) // Pass dateFormat to ViewHolder
    }

    override fun onBindViewHolder(holder: ActiveRentalViewHolder, position: Int) {
        val activeRental = getItem(position)
        holder.bind(activeRental, onItemReturnedClicked)
    }

    // ViewHolder holds references to the views for each item.
    inner class ActiveRentalViewHolder(
        private val binding: ItemActiveRentalBinding,
        private val dateFormat: SimpleDateFormat // Store dateFormat in ViewHolder
        ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            activeRentalInfo: ActiveRentalInfo,
            onItemReturnedClicked: (ActiveRentalInfo) -> Unit
        ) {
            binding.textViewToolNameActiveItem.text = activeRentalInfo.toolName
            binding.textViewCustomerNameActiveItem.text = "Customer: ${activeRentalInfo.customerName}"
            binding.textViewRentalDateActiveItem.text = "Rented: ${dateFormat.format(activeRentalInfo.rentalDate)}"
            binding.textViewDueDateActiveItem.text = "Due: ${dateFormat.format(activeRentalInfo.dueDate)}"

            binding.buttonMarkReturnedItem.setOnClickListener {
                onItemReturnedClicked(activeRentalInfo)
            }
        }
    }

    // DiffCallback efficiently calculates differences between lists for smooth updates.
    companion object ActiveRentalDiffCallback : DiffUtil.ItemCallback<ActiveRentalInfo>() {
        override fun areItemsTheSame(oldItem: ActiveRentalInfo, newItem: ActiveRentalInfo): Boolean {
            // Check if items represent the same entity (e.g., by ID)
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: ActiveRentalInfo, newItem: ActiveRentalInfo): Boolean {
            // Check if the content of the items is the same (relies on data class `equals` implementation)
            return oldItem == newItem
        }
    }
}
