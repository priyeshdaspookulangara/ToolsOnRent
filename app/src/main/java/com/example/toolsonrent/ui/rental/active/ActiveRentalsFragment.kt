package com.example.toolsonrent.ui.rental.active

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.toolsonrent.databinding.FragmentActiveRentalsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder // New import
import kotlinx.coroutines.launch
import java.util.Date // New import for using Date()

class ActiveRentalsFragment : Fragment() {

    private var _binding: FragmentActiveRentalsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ActiveRentalsViewModel
    private lateinit var activeRentalsListAdapter: ActiveRentalsListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveRentalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[ActiveRentalsViewModel::class.java]

        activeRentalsListAdapter = ActiveRentalsListAdapter { activeRentalInfo ->
            // onItemReturnedClicked lambda now calls showConfirmReturnDialog
            showConfirmReturnDialog(activeRentalInfo)
        }

        setupRecyclerView()
        observeActiveRentals()
        observeRentalCompletion() // Call new observer
    }

    private fun setupRecyclerView() {
        binding.recyclerViewActiveRentals.apply {
            adapter = activeRentalsListAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeActiveRentals() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeRentalItems.collect { activeRentals ->
                    activeRentalsListAdapter.submitList(activeRentals)
                    binding.recyclerViewActiveRentals.isVisible = activeRentals.isNotEmpty()
                    binding.textViewNoActiveRentals.isVisible = activeRentals.isEmpty()
                }
            }
        }
    }

    private fun showConfirmReturnDialog(activeRentalInfo: ActiveRentalInfo) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Return")
            .setMessage("Are you sure you want to mark '${activeRentalInfo.toolName}' (rented by ${activeRentalInfo.customerName}) as returned?")
            .setNegativeButton("Cancel", null) // null listener dismisses the dialog
            .setPositiveButton("Confirm Return") { _, _ ->
                // User confirmed, call ViewModel to complete the rental
                viewModel.completeRental(
                    transactionId = activeRentalInfo.transactionId,
                    toolId = activeRentalInfo.toolId,
                    returnDate = Date() // Use current date/time as the return date
                )
            }
            .show()
    }

    private fun observeRentalCompletion() {
        viewModel.rentalCompletionResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Tool returned successfully!", Toast.LENGTH_SHORT).show()
                    // The list of active rentals will refresh automatically because
                    // activeRentalItems StateFlow in ViewModel will emit a new list
                    // after the underlying data changes (transaction updated, tool updated).
                },
                onFailure = { exception ->
                    Log.e("ActiveRentalsFragment", "Error completing rental", exception)
                    Toast.makeText(requireContext(), "Error returning tool: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewActiveRentals.adapter = null
        _binding = null
    }
}
