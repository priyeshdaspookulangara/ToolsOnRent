package com.example.toolsonrent.ui.customer.edit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.toolsonrent.R // For R.id.customerListFragment if used with popBackStack
import com.example.toolsonrent.database.entity.Customer // For populating form
import com.example.toolsonrent.databinding.FragmentEditCustomerBinding // Generated
import com.google.android.material.dialog.MaterialAlertDialogBuilder // New import
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
// No need to import java.util.Date for this fragment's delete logic as it's handled by ViewModel

class EditCustomerFragment : Fragment() {

    private var _binding: FragmentEditCustomerBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Use Safe Args delegate to retrieve customerId passed via navigation.
    private val args: EditCustomerFragmentArgs by navArgs()
    private lateinit var viewModel: EditCustomerViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditCustomerBinding.inflate(inflater, container, false)
        // Initialize ViewModel here. ViewModelProvider(this) ensures the ViewModel is scoped
        // to this Fragment and correctly receives SavedStateHandle with navArgs.
        viewModel = ViewModelProvider(this)[EditCustomerViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeCustomerDetails()
        setupSaveButton()
        setupDeleteButton() // Updated to call showConfirmDeleteDialog
        observeUpdateResult()
        observeDeleteResult() // New observer call
    }

    private fun observeCustomerDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.customer.collectLatest { customer ->
                    customer?.let { populateForm(it) }
                }
            }
        }
    }

    private fun populateForm(customer: Customer) {
        binding.editTextCustomerNameEdit.setText(customer.name)
        binding.editTextCustomerPhoneEdit.setText(customer.phoneNumber)
        binding.editTextCustomerEmailEdit.setText(customer.email ?: "")
        binding.editTextCustomerAddressEdit.setText(customer.address ?: "")
    }

    private fun setupSaveButton() {
        binding.buttonSaveChangesCustomer.setOnClickListener {
            val name = binding.editTextCustomerNameEdit.text.toString().trim()
            val phone = binding.editTextCustomerPhoneEdit.text.toString().trim()
            val email = binding.editTextCustomerEmailEdit.text.toString().trim()
            val address = binding.editTextCustomerAddressEdit.text.toString().trim()

            clearAllErrors()
            viewModel.updateCustomer(args.customerId, name, phone, email, address)
        }
    }

    private fun setupDeleteButton() {
        binding.buttonDeleteCustomer.setOnClickListener {
            showConfirmDeleteDialog() // Call confirmation dialog
        }
    }

    private fun showConfirmDeleteDialog() {
        val customerName = viewModel.customer.value?.name ?: "this customer"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete $customerName? This action cannot be undone. Ensure this customer has no pending rentals or rental history you wish to keep associated, as deletion might fail or lead to data inconsistencies if rentals exist.")
            .setNegativeButton("Cancel", null) // null listener dismisses the dialog
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCustomer() // Call ViewModel to delete
            }
            .show()
    }

    private fun observeUpdateResult() {
        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Customer updated successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditCustomerFragment", "Error updating customer", exception)
                    handleUpdateError(exception)
                }
            )
        }
    }

    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Customer deleted successfully.", Toast.LENGTH_SHORT).show()
                    // Navigate back to the customer list. The list should refresh automatically.
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditCustomerFragment", "Error deleting customer", exception)
                    val errorMessage = if (exception.message?.contains("FOREIGN KEY constraint failed", ignoreCase = true) == true) {
                        "Cannot delete customer. They may have existing rental transactions. Please resolve these first or ensure all rentals are returned and history is no longer critical."
                    } else {
                        exception.message ?: "Unknown error deleting customer."
                    }
                    // Show a more user-friendly dialog for critical errors like FK constraint
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Deletion Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            )
        }
    }

    private fun handleUpdateError(exception: Throwable) {
        val message = exception.message ?: "An unknown error occurred."
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        if (exception is IllegalArgumentException && message.contains("Name and Phone")) {
            if (binding.editTextCustomerNameEdit.text.isNullOrBlank()) {
                binding.textFieldLayoutCustomerNameEdit.error = "Name cannot be empty"
            }
            if (binding.editTextCustomerPhoneEdit.text.isNullOrBlank()) {
                binding.textFieldLayoutCustomerPhoneEdit.error = "Phone cannot be empty"
            }
        } else if (exception is IllegalStateException && message.contains("Invalid Customer ID")) {
            Log.e("EditCustomerFragment", "Attempted to update with an invalid Customer ID.")
        }
    }

    private fun clearAllErrors(){
        binding.textFieldLayoutCustomerNameEdit.error = null
        binding.textFieldLayoutCustomerPhoneEdit.error = null
        binding.textFieldLayoutCustomerEmailEdit.error = null
        binding.textFieldLayoutCustomerAddressEdit.error = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
