package com.example.toolsonrent.ui.reports.customerhistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For R.drawable.ic_customer_placeholder
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.databinding.ItemCustomerBinding // Reusing the item binding from general customer list

class SelectCustomerAdapter(
    private val onCustomerSelected: (Customer) -> Unit
) : ListAdapter<Customer, SelectCustomerAdapter.SelectCustomerViewHolder>(SelectCustomerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectCustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Pass the onCustomerSelected lambda to the ViewHolder
        return SelectCustomerViewHolder(binding, onCustomerSelected)
    }

    override fun onBindViewHolder(holder: SelectCustomerViewHolder, position: Int) {
        val customer = getItem(position)
        holder.bind(customer)
    }

    inner class SelectCustomerViewHolder(
        private val binding: ItemCustomerBinding,
        private val onCustomerSelectedCallback: (Customer) -> Unit // Callback to be invoked on item click
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCustomer: Customer? = null

        init {
            // Set the click listener on the itemView.
            // This listener will use the `currentCustomer` that is set in the `bind` method.
            itemView.setOnClickListener {
                currentCustomer?.let { customer ->
                    onCustomerSelectedCallback(customer)
                }
            }
        }

        fun bind(customer: Customer) {
            this.currentCustomer = customer // Update the current customer reference

            binding.textViewCustomerNameItem.text = customer.name
            binding.textViewCustomerPhoneItem.text = customer.phoneNumber
            // Use the placeholder icon for all customers in this selection list.
            binding.imageViewCustomerIconItem.setImageResource(R.drawable.ic_customer_placeholder)
        }
    }

    companion object SelectCustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            // Efficiently check if items are the same entity by comparing their unique IDs.
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            // Check if the content of the items is the same.
            // This relies on Customer being a data class, which implements equals() correctly.
            return oldItem == newItem
        }
    }
}
