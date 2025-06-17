package com.example.toolsonrent.ui.customer.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.toolsonrent.R // For drawable
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.databinding.ItemCustomerBinding // Generated

class CustomerListAdapter : ListAdapter<Customer, CustomerListAdapter.CustomerViewHolder>(CustomerDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(customer: Customer) {
            binding.textViewCustomerNameItem.text = customer.name
            binding.textViewCustomerPhoneItem.text = customer.phoneNumber
            // For now, use the placeholder icon for all
            binding.imageViewCustomerIconItem.setImageResource(R.drawable.ic_customer_placeholder)

            // Example: Set an OnClickListener for item interaction
            // itemView.setOnClickListener {
            //     onItemClickListener?.invoke(customer)
            // }
        }
    }

    // Optional: Listener for item clicks, can be set from the Fragment/Activity
    // var onItemClickListener: ((Customer) -> Unit)? = null

    companion object CustomerDiffCallback : DiffUtil.ItemCallback<Customer>() {
        override fun areItemsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Customer, newItem: Customer): Boolean {
            return oldItem == newItem // Assumes Customer is a data class
        }
    }
}
