package com.example.toolsonrent.ui.reports.overdue

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible // For isVisible extension function
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
// ItemDecoration can be added for visual separation if desired
// import androidx.recyclerview.widget.DividerItemDecoration
import com.example.toolsonrent.databinding.FragmentOverdueRentalsReportBinding // Generated
// OverdueRentalsReportViewModel and OverdueRentalsListAdapter are in the same package.
// OverdueRentalInfo is also in the same package.
import kotlinx.coroutines.launch

class OverdueRentalsReportFragment : Fragment() {

    private var _binding: FragmentOverdueRentalsReportBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: OverdueRentalsReportViewModel
    private lateinit var overdueRentalsListAdapter: OverdueRentalsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOverdueRentalsReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[OverdueRentalsReportViewModel::class.java]

        // Initialize the adapter. No item click listener needed for this report view for now.
        overdueRentalsListAdapter = OverdueRentalsListAdapter()

        setupRecyclerView()
        observeOverdueRentals()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewOverdueRentals.apply {
            adapter = overdueRentalsListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Example of adding a divider:
            // val divider = DividerItemDecoration(requireContext(), (layoutManager as LinearLayoutManager).orientation)
            // addItemDecoration(divider)
        }
    }

    private fun observeOverdueRentals() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection starts when fragment is STARTED and stops when STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.overdueRentalItems.collect { overdueRentals ->
                    overdueRentalsListAdapter.submitList(overdueRentals)
                    // Toggle visibility of the RecyclerView and the "no overdue rentals" message.
                    binding.recyclerViewOverdueRentals.isVisible = overdueRentals.isNotEmpty()
                    binding.textViewNoOverdueRentals.isVisible = overdueRentals.isEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter reference from RecyclerView to prevent potential memory leaks,
        // especially if the RecyclerView or its adapter hold references to the Fragment's view.
        binding.recyclerViewOverdueRentals.adapter = null
        _binding = null // Release the binding instance
    }
}
