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

import com.bumptech.glide.Glide // Added for image loading
import com.example.toolsonrent.R // Added for string resources

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
            binding.textViewToolNameActiveItem.text = activeRentalInfo.toolTypeName
            binding.textViewToolInstanceIdentifierActiveItem.text = activeRentalInfo.toolInstanceIdentifier
            binding.textViewCustomerNameActiveItem.text = itemView.context.getString(R.string.customer_label_prefix, activeRentalInfo.customerName)
            binding.textViewRentalDateActiveItem.text = itemView.context.getString(R.string.rented_date_prefix, dateFormat.format(activeRentalInfo.rentalDate))
            binding.textViewDueDateActiveItem.text = itemView.context.getString(R.string.due_date_prefix, dateFormat.format(activeRentalInfo.dueDate))

            Glide.with(itemView.context)
                .load(activeRentalInfo.toolImageUri)
                .placeholder(R.drawable.ic_image_placeholder) // Ensure this placeholder exists
                .error(R.drawable.ic_broken_image) // Ensure this error drawable exists
                .into(binding.imageViewToolTypeActiveItem)

            binding.buttonMarkReturnedItem.setOnClickListener {
                onItemReturnedClicked(activeRentalInfo)
            }
        }
    }

    // DiffCallback efficiently calculates differences between lists for smooth updates.
    companion object ActiveRentalDiffCallback : DiffUtil.ItemCallback<ActiveRentalInfo>() {
        override fun areItemsTheSame(oldItem: ActiveRentalInfo, newItem: ActiveRentalInfo): Boolean {
            return oldItem.transactionId == newItem.transactionId
        }

        override fun areContentsTheSame(oldItem: ActiveRentalInfo, newItem: ActiveRentalInfo): Boolean {
            return oldItem == newItem
        }
    }
}
// Need to add string resources:
// <string name="customer_label_prefix">Customer: %1$s</string>
// <string name="rented_date_prefix">Rented: %1$s</string>
// <string name="due_date_prefix">Due: %1$s</string>
