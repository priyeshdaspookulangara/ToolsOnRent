package com.example.toolsonrent.ui.toollistscreen

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
// import androidx.recyclerview.widget.DividerItemDecoration // Optional
import com.example.toolsonrent.R // For R.id.action_...
import com.example.toolsonrent.database.entity.Tool // For type in lambda
import com.example.toolsonrent.databinding.FragmentToolListBinding
import kotlinx.coroutines.launch

class ToolListFragment : Fragment() {

    private var _binding: FragmentToolListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ToolListViewModel
    private lateinit var toolListAdapter: ToolListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ToolListViewModel::class.java]

        toolListAdapter = ToolListAdapter { selectedTool ->
            // Navigate to EditToolFragment, passing toolId
            // This assumes a Safe Args action is defined in nav_graph.xml
            // e.g., ToolListFragmentDirections.actionToolListFragmentToEditToolFragment(selectedTool.id)
            try {
                val action = ToolListFragmentDirections
                                 .actionToolListFragmentToEditToolFragment(selectedTool.id)
                findNavController().navigate(action)
            } catch (e: Exception) { // Catch generic Exception as SafeArgs might not be generated yet
                Log.e("ToolListFragment", "Navigation to EditToolFragment failed. Safe Args or NavGraph issue.", e)
                Toast.makeText(requireContext(), "Error: Could not open tool details.", Toast.LENGTH_SHORT).show()
                // Fallback for development if Safe Args isn't immediately available:
                // val bundle = Bundle().apply { putInt("toolId", selectedTool.id) }
                // findNavController().navigate(R.id.action_toolListFragment_to_editToolFragment, bundle)
            }
        }

        setupRecyclerView()
        observeTools()
        setupFab()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewTools.apply {
            adapter = toolListAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Optional: Add item decoration for visual separation. Uncomment to use.
            // addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        }
    }

    private fun observeTools() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTools.collect { toolsList ->
                    toolListAdapter.submitList(toolsList)
                    // Update visibility for RecyclerView (if an empty state TextView were present, it would be toggled here too)
                    binding.recyclerViewTools.isVisible = toolsList.isNotEmpty()
                    // Example if textViewNoTools existed:
                    // binding.textViewNoTools.isVisible = toolsList.isEmpty()
                }
            }
        }
    }

    private fun setupFab() {
        binding.fabAddTool.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_toolListFragment_to_addToolFragment)
            } catch (e: IllegalArgumentException) {
                Log.e("ToolListFragment", "Navigation action R.id.action_toolListFragment_to_addToolFragment not found.", e)
                Toast.makeText(context, "Action to add tool not found. Check navigation graph.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewTools.adapter = null // Clear adapter to help prevent memory leaks
        _binding = null
    }
}
