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

    // To allow choosing a status on return
    private val returnStatuses = arrayOf("Available", "Needs Maintenance", "Damaged")


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
            showConfirmReturnDialogWithOptions(activeRentalInfo) // Updated to new dialog
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

    private fun showConfirmReturnDialogWithOptions(activeRentalInfo: ActiveRentalInfo) {
        val localizedReturnStatuses = returnStatuses.map { getLocalizedStatus(it) }.toTypedArray()
        var selectedStatusKey = returnStatuses[0] // Default to "Available"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_return_title))
            .setMessage(getString(R.string.confirm_return_message, activeRentalInfo.toolTypeName, activeRentalInfo.toolInstanceIdentifier, activeRentalInfo.customerName))
            .setSingleChoiceItems(localizedReturnStatuses, 0) { _, which ->
                selectedStatusKey = returnStatuses[which] // Map localized choice back to key
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_confirm_return) { _, _ ->
                viewModel.completeRental(
                    transactionId = activeRentalInfo.transactionId,
                    toolInstanceId = activeRentalInfo.toolInstanceId, // Use toolInstanceId
                    returnDate = Date(),
                    newStatus = selectedStatusKey // Pass the selected status
                )
            }
            .show()
    }

    // Helper to get localized status strings (similar to Add/EditToolInstanceFragment)
    // This could be moved to a shared utility or string resource mapping if used in many places.
    private fun getLocalizedStatus(statusKey: String): String {
        return when (statusKey) {
            "Available" -> getString(R.string.label_status_available)
            "Needs Maintenance" -> getString(R.string.label_status_maintenance) // Assuming this string exists
            "Damaged" -> getString(R.string.label_status_damaged)
            // Add other statuses if needed
            else -> statusKey
        }
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
