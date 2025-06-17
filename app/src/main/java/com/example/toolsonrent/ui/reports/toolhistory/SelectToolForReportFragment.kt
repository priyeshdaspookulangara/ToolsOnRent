package com.example.toolsonrent.ui.reports.toolhistory

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
import com.example.toolsonrent.R // For R.id if not using Safe Args immediately
import com.example.toolsonrent.database.entity.Tool // Explicit import for lambda param type
import com.example.toolsonrent.databinding.FragmentSelectToolForReportBinding // Generated
// ViewModel and Adapter are in the same package.
import kotlinx.coroutines.launch

class SelectToolForReportFragment : Fragment() {

    private var _binding: FragmentSelectToolForReportBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: SelectToolForReportViewModel
    private lateinit var selectToolAdapter: SelectToolAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSelectToolForReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SelectToolForReportViewModel::class.java]

        selectToolAdapter = SelectToolAdapter { selectedTool ->
            // This is the onToolSelected lambda.
            // It will navigate to the ToolRentalHistoryFragment, passing the tool's ID.
            // The navigation action 'action_selectToolForReportFragment_to_toolRentalHistoryFragment'
            // and its argument 'toolId' need to be defined in the nav_graph.xml.
            // We are anticipating the use of Safe Args generated NavDirections.
            try {
                 val action = SelectToolForReportFragmentDirections
                                 .actionSelectToolForReportFragmentToToolRentalHistoryFragment(selectedTool.id)
                 findNavController().navigate(action)
            } catch (e: Exception) {
                // Catching generic Exception as SafeArgs classes might not be generated if nav graph isn't updated,
                // or if there's an unexpected issue with navigation component setup.
                Log.e("SelectToolFragment", "Navigation failed. Safe Args action or destination may not be found. Check nav_graph.xml.", e)
                Toast.makeText(requireContext(), "Error: Cannot navigate to tool history. Feature setup might be pending.", Toast.LENGTH_LONG).show()
                // Fallback for development if Safe Args isn't immediately available:
                // val bundle = Bundle().apply { putInt("toolId", selectedTool.id) }
                // findNavController().navigate(R.id.action_selectToolForReportFragment_to_toolRentalHistoryFragment, bundle)
            }
        }

        setupRecyclerView()
        observeToolList()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewSelectTool.apply {
            adapter = selectToolAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Consider ItemDecoration if needed
        }
    }

    private fun observeToolList() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTools.collect { tools ->
                    selectToolAdapter.submitList(tools)
                    // Update visibility of the empty state TextView and RecyclerView.
                    binding.textViewNoToolsToSelect.isVisible = tools.isEmpty()
                    binding.recyclerViewSelectTool.isVisible = tools.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter to help prevent memory leaks with RecyclerView.
        binding.recyclerViewSelectTool.adapter = null
        _binding = null // Release the binding instance.
    }
}
