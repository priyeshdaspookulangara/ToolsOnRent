package com.example.toolsonrent.ui.reports.toolhistory

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
import com.example.toolsonrent.database.AppDatabase // For ToolDao
import com.example.toolsonrent.database.entity.Tool // For Tool type in DAO
import com.example.toolsonrent.databinding.FragmentToolRentalHistoryBinding // Generated
// ViewModel, Adapter, ToolRentalHistoryItem are in the same package.
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ToolRentalHistoryFragment : Fragment() {

    private var _binding: FragmentToolRentalHistoryBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Use Safe Args delegate to retrieve toolId passed via navigation.
    private val args: ToolRentalHistoryFragmentArgs by navArgs()

    private lateinit var viewModel: ToolRentalHistoryViewModel
    private lateinit var toolHistoryAdapter: ToolRentalHistoryAdapter
    // DAO instance to fetch tool details for the title.
    private lateinit var toolDao: com.example.toolsonrent.database.dao.ToolDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolRentalHistoryBinding.inflate(inflater, container, false)

        // Initialize ViewModel. ViewModelProvider(this) ensures the ViewModel is scoped
        // to this Fragment and can correctly receive SavedStateHandle with navArgs.
        viewModel = ViewModelProvider(this)[ToolRentalHistoryViewModel::class.java]

        // Initialize ToolDao.
        toolDao = AppDatabase.getInstance(requireContext().applicationContext).toolDao()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolHistoryAdapter = ToolRentalHistoryAdapter() // Adapter for the RecyclerView.

        // Fetch the tool's name using the toolId from navArgs and set the title.
        fetchToolNameAndSetTitle(args.toolId)

        setupRecyclerView()
        observeRentalHistory()
    }

    private fun fetchToolNameAndSetTitle(toolId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            // This is a one-time fetch for the title.
            val tool = toolDao.getToolById(toolId).firstOrNull()
            binding.textViewToolHistoryTitle.text =
                "Rental History for ${tool?.name ?: "Tool ID: $toolId"}"
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewToolHistory.apply {
            adapter = toolHistoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Consider adding ItemDecoration for visual separation if desired.
        }
    }

    private fun observeRentalHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            // repeatOnLifecycle ensures collection starts when fragment is STARTED and stops when STOPPED.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rentalHistoryItems.collect { historyItems ->
                    toolHistoryAdapter.submitList(historyItems)
                    // Update visibility of the empty state TextView and RecyclerView.
                    binding.textViewNoToolHistoryFound.isVisible = historyItems.isEmpty()
                    binding.recyclerViewToolHistory.isVisible = historyItems.isNotEmpty()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter to help prevent memory leaks with RecyclerView.
        binding.recyclerViewToolHistory.adapter = null
        _binding = null // Release the binding instance.
    }
}
