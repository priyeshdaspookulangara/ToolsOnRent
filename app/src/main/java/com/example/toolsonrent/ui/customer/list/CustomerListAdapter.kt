package com.example.toolsonrent.ui.customer.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For drawable
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.databinding.ItemCustomerBinding // Generated

class CustomerListAdapter(
    private val onCustomerClicked: (Customer) -> Unit // New parameter for click callback
) : ListAdapter<Customer, CustomerListAdapter.CustomerViewHolder>(CustomerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        // Pass the onCustomerClicked lambda to the ViewHolder
        return CustomerViewHolder(binding, onCustomerClicked)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = getItem(position)
        holder.bind(customer)
    }

    inner class CustomerViewHolder(
        private val binding: ItemCustomerBinding,
        private val onCustomerClickedCallback: (Customer) -> Unit // Renamed for clarity within ViewHolder
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentCustomer: Customer? = null // Hold current customer for the click listener

        init {
            itemView.setOnClickListener {
                currentCustomer?.let { customer ->
                    onCustomerClickedCallback(customer)
                }
            }
        }

        fun bind(customer: Customer) {
            currentCustomer = customer // Store customer for the click listener
            binding.textViewCustomerNameItem.text = customer.name
            binding.textViewCustomerPhoneItem.text = customer.phoneNumber
            // For now, use the placeholder icon for all
            binding.imageViewCustomerIconItem.setImageResource(R.drawable.ic_customer_placeholder)
        }
    }

    // Removed the old commented-out onItemClickListener property

    companion object CustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem == newItem // Assumes Customer is a data class
        }
    }
}
