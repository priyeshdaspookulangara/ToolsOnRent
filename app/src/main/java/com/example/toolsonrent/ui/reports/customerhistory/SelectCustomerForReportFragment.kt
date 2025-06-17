package com.example.toolsonrent.ui.reports.customerhistory

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.R // For navigation action ID if not using Safe Args immediately
import com.example.toolsonrent.database.entity.Customer // Explicit import for lambda param type
import com.example.toolsonrent.databinding.FragmentSelectCustomerForReportBinding // Generated
// ViewModel, Adapter, are in the same package.
import kotlinx.coroutines.launch

class SelectCustomerForReportFragment : Fragment() {

    private var _binding: FragmentSelectCustomerForReportBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: SelectCustomerForReportViewModel
    private lateinit var selectCustomerAdapter: SelectCustomerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectCustomerForReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SelectCustomerForReportViewModel::class.java]

        selectCustomerAdapter = SelectCustomerAdapter { selectedCustomer ->
            // This is the onCustomerSelected lambda.
            // It will navigate to the CustomerRentalHistoryFragment, passing the customer's ID.
            // The navigation action 'action_selectCustomerForReportFragment_to_customerRentalHistoryFragment'
            // and its argument 'customerId' need to be defined in the nav_graph.xml.
            // We are anticipating the use of Safe Args generated NavDirections.
            // If Safe Args plugin is not configured, this line will cause a build error until it is,
            // or until changed to manual navigation with a Bundle.
            try {
                 val action = SelectCustomerForReportFragmentDirections
                                 .actionSelectCustomerForReportFragmentToCustomerRentalHistoryFragment(selectedCustomer.id)
                 findNavController().navigate(action)
            } catch (e: Exception) {
                // Fallback or error logging if Safe Args generated class isn't found or other nav issue
                Log.e("SelectCustomerFragment", "Navigation failed, Safe Args action likely not generated or nav graph issue.", e)
                Toast.makeText(requireContext(), "Failed to navigate to customer history. Feature might be under setup.", Toast.LENGTH_LONG).show()
                // As a temporary fallback if Safe Args isn't immediately available during generation:
                // findNavController().navigate(R.id.action_selectCustomerForReportFragment_to_customerRentalHistoryFragment, bundleOf("customerId" to selectedCustomer.id))
            }
        }

        setupRecyclerView()
        observeCustomerList()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSelectCustomer.apply {
            adapter = selectCustomerAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Consider adding ItemDecoration for visual separation if desired.
        }
    }

    private fun observeCustomerList() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCustomers.collect { customers ->
                    selectCustomerAdapter.submitList(customers)
                    // Update visibility of the empty state TextView and RecyclerView.
                    binding.textViewNoCustomersToSelect.isVisible = customers.isEmpty()
                    binding.recyclerViewSelectCustomer.isVisible = customers.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter to help prevent memory leaks with RecyclerView.
        binding.recyclerViewSelectCustomer.adapter = null
        _binding = null // Release the binding instance.
    }
}
