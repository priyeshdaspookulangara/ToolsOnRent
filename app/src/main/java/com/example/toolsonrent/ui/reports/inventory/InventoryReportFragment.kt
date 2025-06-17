package com.example.toolsonrent.ui.reports.inventory

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
import androidx.recyclerview.widget.LinearLayoutManager
// ItemDecoration can be added for visual separation if desired
// import androidx.recyclerview.widget.DividerItemDecoration
import com.example.toolsonrent.databinding.FragmentInventoryReportBinding // Generated
import com.example.toolsonrent.ui.toollistscreen.ToolListAdapter // Reusing this adapter
// InventoryReportViewModel is in the same package.
import kotlinx.coroutines.launch

class InventoryReportFragment : Fragment() {

    private var _binding: FragmentInventoryReportBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: InventoryReportViewModel
    private lateinit var toolListAdapter: ToolListAdapter // Reusing ToolListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[InventoryReportViewModel::class.java]

        // Initialize the ToolListAdapter.
        // If ToolListAdapter had a click listener, it would be passed here.
        // For this report, items are display-only from the list perspective.
        toolListAdapter = ToolListAdapter()

        setupRecyclerView()
        observeInventoryData()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewInventoryReport.apply {
            adapter = toolListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Example of adding a divider:
            // val divider = DividerItemDecoration(requireContext(), (layoutManager as LinearLayoutManager).orientation)
            // addItemDecoration(divider)
        }
    }

    private fun observeInventoryData() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection starts when fragment is STARTED and stops when STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observe total tools count
                launch {
                    viewModel.totalToolsCount.collect { count ->
                        binding.textViewTotalToolsInvValue.text = count.toString()
                    }
                }

                // Observe available tools count
                launch {
                    viewModel.availableToolsCount.collect { count ->
                        binding.textViewAvailableToolsInvValue.text = count.toString()
                    }
                }

                // Observe rented tools count
                launch {
                    viewModel.rentedToolsCount.collect { count ->
                        binding.textViewRentedToolsInvValue.text = count.toString()
                    }
                }

                // Observe the list of all tools for the RecyclerView
                launch {
                    viewModel.allToolsList.collect { tools ->
                        toolListAdapter.submitList(tools)
                        // Toggle visibility of the RecyclerView and the "no tools" message.
                        val listIsEmpty = tools.isEmpty()
                        binding.textViewNoToolsInventory.isVisible = listIsEmpty
                        binding.recyclerViewInventoryReport.isVisible = !listIsEmpty
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter reference from RecyclerView to prevent potential memory leaks.
        binding.recyclerViewInventoryReport.adapter = null
        _binding = null // Release the binding instance.
    }
}
