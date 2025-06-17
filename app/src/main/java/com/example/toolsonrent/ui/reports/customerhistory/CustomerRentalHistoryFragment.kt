package com.example.toolsonrent.ui.reports.customerhistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs // For Safe Args
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.database.AppDatabase // For CustomerDao
import com.example.toolsonrent.databinding.FragmentCustomerRentalHistoryBinding // Generated
// ViewModel, Adapter, CustomerRentalHistoryItem are in the same package.
// Customer entity might be needed if not inferred by customerDao.getCustomerById
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class CustomerRentalHistoryFragment : Fragment() {

    private var _binding: FragmentCustomerRentalHistoryBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Use Safe Args delegate to retrieve customerId passed via navigation.
    private val args: CustomerRentalHistoryFragmentArgs by navArgs()

    private lateinit var viewModel: CustomerRentalHistoryViewModel
    private lateinit var customerHistoryAdapter: CustomerRentalHistoryAdapter
    // DAO instance to fetch customer details for the title.
    private lateinit var customerDao: com.example.toolsonrent.database.dao.CustomerDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerRentalHistoryBinding.inflate(inflater, container, false)

        // Initialize ViewModel. The ViewModelProvider, by default, can handle SavedStateHandle
        // for ViewModels that take it as a constructor parameter, especially when using navArgs.
        viewModel = ViewModelProvider(this)[CustomerRentalHistoryViewModel::class.java]

        // Initialize CustomerDao.
        customerDao = AppDatabase.getInstance(requireContext().applicationContext).customerDao()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        customerHistoryAdapter = CustomerRentalHistoryAdapter() // Adapter for the RecyclerView.

        // Fetch the customer's name using the customerId from navArgs and set the title.
        fetchCustomerNameAndSetTitle(args.customerId)

        setupRecyclerView()
        observeRentalHistory()
    }

    private fun fetchCustomerNameAndSetTitle(customerId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            // This is a one-time fetch for the title.
            // If the customer's name could change while this screen is visible,
            // this would need to be a collecting Flow as well.
            val customer = customerDao.getCustomerById(customerId).firstOrNull()
            binding.textViewCustomerHistoryTitle.text =
                "Rental History for ${customer?.name ?: "Customer (ID: $customerId)"}"
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewCustomerHistory.apply {
            adapter = customerHistoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Consider adding ItemDecoration for visual separation if desired.
        }
    }

    private fun observeRentalHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection starts when fragment is STARTED and stops when STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rentalHistoryItems.collect { historyItems ->
                    customerHistoryAdapter.submitList(historyItems)
                    // Update visibility of the empty state TextView and RecyclerView.
                    binding.textViewNoHistoryFound.isVisible = historyItems.isEmpty()
                    binding.recyclerViewCustomerHistory.isVisible = historyItems.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter to help prevent memory leaks with RecyclerView.
        binding.recyclerViewCustomerHistory.adapter = null
        _binding = null // Release the binding instance.
    }
}
