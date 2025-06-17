package com.example.toolsonrent.ui.customer.list

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.R // For nav action ID
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
        viewModel = ViewModelProvider(this)[CustomerListViewModel::class.java]
        customerListAdapter = CustomerListAdapter()
        // Consider setting onItemClickListener for the adapter here if needed
        // customerListAdapter.onItemClickListener = { customer ->
        //    val action = CustomerListFragmentDirections.actionCustomerListFragmentToCustomerDetailFragment(customer.id)
        //    findNavController().navigate(action)
        // }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddCustomer.setOnClickListener {
            try {
                // This action ID needs to be defined in nav_graph.xml
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
