package com.example.toolsonrent.ui.toolinstance.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentToolInstanceListBinding
import com.google.android.material.snackbar.Snackbar

class ToolInstanceListFragment : Fragment() {

    private var _binding: FragmentToolInstanceListBinding? = null
    private val binding get() = _binding!!

    private val args: ToolInstanceListFragmentArgs by navArgs()
    private lateinit var viewModel: ToolInstanceListViewModel
    private lateinit var adapter: ToolInstanceListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolInstanceListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = ToolInstanceListViewModelFactory(requireActivity().application, args.toolTypeId)
        viewModel = ViewModelProvider(this, factory)[ToolInstanceListViewModel::class.java]

        setupRecyclerView()
        observeViewModel()

        binding.fabAddToolInstance.setOnClickListener {
            // Navigate to AddToolInstanceFragment, passing toolTypeId
            val action = ToolInstanceListFragmentDirections.actionToolInstanceListFragmentToAddToolInstanceFragment(args.toolTypeId)
            findNavController().navigate(action)
        }

        // Update toolbar title with the Tool's name
        viewModel.tool.observe(viewLifecycleOwner) { tool ->
            tool?.let {
                (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_tool_instance_list_with_name, it.name)
            } ?: run {
                (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_tool_instance_list)
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = ToolInstanceListAdapter { toolInstance ->
            // Navigate to EditToolInstanceFragment, passing instanceId
            val action = ToolInstanceListFragmentDirections.actionToolInstanceListFragmentToEditToolInstanceFragment(toolInstance.instanceId)
            findNavController().navigate(action)
        }
        binding.recyclerViewToolInstances.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewToolInstances.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.toolInstances.observe(viewLifecycleOwner) { instances ->
            if (instances.isNullOrEmpty()) {
                // Show a message if the list is empty (e.g., using a TextView in the layout, or a Snackbar)
                // For now, just log or a simple Snackbar
                Snackbar.make(binding.root, R.string.no_instances_found, Snackbar.LENGTH_LONG).show()
                adapter.submitList(emptyList()) // Ensure adapter is cleared
            } else {
                adapter.submitList(instances)
            }
        }

        // Optionally observe counts if you want to display them
        viewModel.totalInstanceCount.observe(viewLifecycleOwner) { count ->
            // Update UI with total count if needed
        }
        viewModel.availableInstanceCount.observe(viewLifecycleOwner) { count ->
            // Update UI with available count if needed
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
// Notes:
// 1. Need to add ToolInstanceListFragmentArgs to nav_graph.xml for toolTypeId.
// 2. Need to define navigation actions:
//    - actionToolInstanceListFragmentToAddToolInstanceFragment (to AddToolInstanceFragment)
//    - actionToolInstanceListFragmentToEditToolInstanceFragment (to EditToolInstanceFragment)
// 3. Add string: <string name="title_tool_instance_list_with_name">Instances: %1$s</string>
// 4. Ensure the package com.example.toolsonrent.ui.toolinstance.list is correct.
// 5. Assumes AppCompatActivity for supportActionBar.
// 6. Snackbar for empty list is a placeholder; a more integrated empty view would be better.
