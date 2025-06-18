package com.example.toolsonrent.ui.customer.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible // For isVisible extension
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.R // For nav action ID
import com.example.toolsonrent.database.entity.Customer // For type in lambda
import com.example.toolsonrent.databinding.FragmentCustomerListBinding // Generated
import kotlinx.coroutines.launch

class CustomerListFragment : Fragment() {

    private var _binding: FragmentCustomerListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CustomerListViewModel
    private lateinit var customerListAdapter: CustomerListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerListBinding.inflate(inflater, container, false)
        // ViewModel and Adapter initialization moved to onViewCreated for consistency
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CustomerListViewModel::class.java]

        customerListAdapter = CustomerListAdapter { selectedCustomer ->
            // Navigate to EditCustomerFragment, passing customerId
            // This assumes a Safe Args action is defined in nav_graph.xml
            // e.g., CustomerListFragmentDirections.actionCustomerListFragmentToEditCustomerFragment(selectedCustomer.id)
            try {
                val action = CustomerListFragmentDirections
                                 .actionCustomerListFragmentToEditCustomerFragment(selectedCustomer.id)
                findNavController().navigate(action)
            } catch (e: Exception) { // Catch generic Exception as SafeArgs might not be generated yet
                Log.e("CustomerListFragment", "Navigation to EditCustomerFragment failed. Safe Args or NavGraph issue.", e)
                Toast.makeText(requireContext(), "Error: Could not open customer details.", Toast.LENGTH_SHORT).show()
                 // Fallback for development if Safe Args isn't immediately available:
                // val bundle = Bundle().apply { putInt("customerId", selectedCustomer.id) }
                // findNavController().navigate(R.id.action_customerListFragment_to_editCustomerFragment, bundle)
            }
        }

        setupRecyclerView()
        observeCustomerList()
        setupFab()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCustomers.apply {
            adapter = customerListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Optionally, add ItemDecoration
            // addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeCustomerList() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCustomers.collect { customers ->
                    customerListAdapter.submitList(customers)
                    // Update visibility for empty state
                    binding.recyclerViewCustomers.isVisible = customers.isNotEmpty()
                    // Assuming there's a textViewNoCustomers in fragment_customer_list.xml
                    // If not, this line would need an ID from that layout or be removed.
                    // Let's assume it was intended to be added or exists.
                    // binding.textViewNoCustomers.isVisible = customers.isEmpty()
                    // Based on fragment_customer_list.xml, there is no specific empty text view,
                    // but if we were to add one, this is where its visibility would be toggled.
                    // For now, the list will just be empty if no customers.
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddCustomer.setOnClickListener {
            try {
                // This action ID (action_customerListFragment_to_addCustomerFragment)
                // should already be defined in nav_graph.xml from previous steps.
                findNavController().navigate(R.id.action_customerListFragment_to_addCustomerFragment)
            } catch (e: IllegalArgumentException) {
                Log.e("CustomerListFragment", "Navigation action R.id.action_customerListFragment_to_addCustomerFragment not found.", e)
                Toast.makeText(context, "Action to add customer not found. Check navigation graph.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewCustomers.adapter = null // Clear adapter to avoid leaks with RecyclerView
        _binding = null
    }
}
