package com.example.toolsonrent.ui.customer.edit

package com.example.toolsonrent.ui.customer.edit

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
// import androidx.navigation.fragment.navArgs // Not strictly needed if ViewModel handles ID from SavedStateHandle
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentEditCustomerBinding
import com.example.toolsonrent.databinding.ItemPhoneEntryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EditCustomerFragment : Fragment() {

    private var _binding: FragmentEditCustomerBinding? = null
    private val binding get() = _binding!!

    // private val args: EditCustomerFragmentArgs by navArgs() // ViewModel now gets customerId from SavedStateHandle
    private lateinit var viewModel: EditCustomerViewModel
    private val phoneTypes = arrayOf("Mobile", "Work", "Home", "Other")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditCustomerBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[EditCustomerViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeViewModelState() // Populates form fields & phone entries
        setupSaveButton()
        setupDeleteButton()
        setupAddPhoneButton()
        setupReferrerSelection()

        observeUpdateResult()
        observeDeleteResult()
    }

    private fun setupAddPhoneButton() {
        binding.buttonAddPhoneNumber.setOnClickListener {
            viewModel.addPhoneNumberField() // ViewModel updates its list, observer below will refresh UI
        }
    }

    private fun observeViewModelState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe individual state flows from ViewModel to populate fields
                launch { viewModel.name.collectLatest { binding.editTextCustomerNameEdit.setText(it) } }
                launch { viewModel.email.collectLatest { binding.editTextCustomerEmailEdit.setText(it ?: "") } }
                launch { viewModel.address.collectLatest { binding.editTextCustomerAddressEdit.setText(it ?: "") } }
                launch { viewModel.jobField.collectLatest { binding.editTextCustomerJobFieldEdit.setText(it ?: "") } }
                launch { viewModel.companyName.collectLatest { binding.editTextCustomerCompanyNameEdit.setText(it ?: "") } }

                launch {
                    viewModel.selectedReferrerId.collectLatest { referrerId ->
                        val referrer = viewModel.potentialReferrers.value.find { it.id == referrerId }
                        binding.textViewSelectedReferrer.text = "Selected Referrer: ${referrer?.name ?: "None"}"
                    }
                }

                // Observe phone numbers list and update UI
                launch {
                    viewModel.phoneNumbers.collectLatest { phoneList ->
                        binding.phoneNumbersContainer.removeAllViews()
                        phoneList.forEachIndexed { index, tempPhone ->
                            val phoneEntryBinding = ItemPhoneEntryBinding.inflate(layoutInflater, binding.phoneNumbersContainer, false)
                            phoneEntryBinding.editTextPhoneNumberItem.setText(tempPhone.number)

                            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, phoneTypes)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            phoneEntryBinding.spinnerPhoneTypeItem.adapter = adapter
                            val typePosition = phoneTypes.indexOf(tempPhone.type).coerceAtLeast(0)
                            phoneEntryBinding.spinnerPhoneTypeItem.setSelection(typePosition)

                            phoneEntryBinding.editTextPhoneNumberItem.setOnFocusChangeListener { _, hasFocus ->
                                if (!hasFocus) {
                                    viewModel.updatePhoneNumberValue(index, phoneEntryBinding.editTextPhoneNumberItem.text.toString())
                                }
                            }
                            phoneEntryBinding.spinnerPhoneTypeItem.onItemSelectedListener =
                                object : android.widget.AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                                        viewModel.updatePhoneType(index, phoneTypes[pos])
                                    }
                                    override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
                                }
                            phoneEntryBinding.buttonRemovePhoneItem.setOnClickListener {
                                viewModel.removePhoneNumberField(tempPhone)
                            }
                            binding.phoneNumbersContainer.addView(phoneEntryBinding.root)
                        }
                    }
                }
            }
        }
    }


    private fun setupSaveButton() {
        binding.buttonSaveChangesCustomer.setOnClickListener {
            collectDataAndUpdateCustomer()
        }
    }

    private fun collectDataAndUpdateCustomer() {
        // Update ViewModel states from UI text fields before saving
        viewModel.name.value = binding.editTextCustomerNameEdit.text.toString().trim()
        viewModel.email.value = binding.editTextCustomerEmailEdit.text.toString().trim().ifBlank { null }
        viewModel.address.value = binding.editTextCustomerAddressEdit.text.toString().trim().ifBlank { null }
        viewModel.jobField.value = binding.editTextCustomerJobFieldEdit.text.toString().trim().ifBlank { null }
        viewModel.companyName.value = binding.editTextCustomerCompanyNameEdit.text.toString().trim().ifBlank { null }

        // Ensure phone numbers in ViewModel are up-to-date from their EditTexts
        for (i in 0 until binding.phoneNumbersContainer.childCount) {
            val phoneEntryView = binding.phoneNumbersContainer.getChildAt(i)
            val editText = phoneEntryView.findViewById<EditText>(R.id.editTextPhoneNumberItem)
            if (i < viewModel.phoneNumbers.value.size) {
                 viewModel.updatePhoneNumberValue(i, editText.text.toString())
            }
        }
        clearAllErrors()
        viewModel.updateCustomer()
    }


    private fun setupDeleteButton() {
        binding.buttonDeleteCustomer.setOnClickListener {
            showConfirmDeleteDialog()
        }
    }

    private fun showConfirmDeleteDialog() {
        // Use name from ViewModel's state flow
        val customerName = viewModel.name.value.takeIf { it.isNotBlank() } ?: "this customer"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Customer")
            .setMessage("Are you sure you want to delete $customerName? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteCustomer() }
            .show()
    }

    private fun setupReferrerSelection() {
        binding.buttonSelectReferrer.setOnClickListener {
            val referrers = viewModel.potentialReferrers.value
            if (referrers.isEmpty()) {
                Toast.makeText(requireContext(), "No potential referrers available.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val referrerNames = referrers.map { it.name }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle("Select Referrer")
                .setItems(referrerNames) { dialog, which ->
                    viewModel.selectedReferrerId.value = referrers[which].id
                    dialog.dismiss()
                }
                .setNegativeButton("Clear Referrer") { dialog, _ ->
                    viewModel.selectedReferrerId.value = null
                    dialog.dismiss()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }
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
                    Toast.makeText(requireContext(), "Update failed: ${exception.message}", Toast.LENGTH_LONG).show()
                     if (binding.editTextCustomerNameEdit.text.isNullOrBlank()) {
                        binding.textFieldLayoutCustomerNameEdit.error = "Name cannot be empty"
                    }
                    // TODO: more specific error handling
                }
            )
        }
    }

    private fun observeDeleteResult() {
        viewModel.deleteResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Customer deleted successfully.", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("EditCustomerFragment", "Error deleting customer", exception)
                    val errorMessage = if (exception.message?.contains("FOREIGN KEY constraint failed", ignoreCase = true) == true) {
                        "Cannot delete customer. They may have existing rental transactions."
                    } else {
                        exception.message ?: "Unknown error."
                    }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Deletion Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            )
        }
    }

    private fun clearAllErrors(){
        binding.textFieldLayoutCustomerNameEdit.error = null
        binding.textFieldLayoutCustomerEmailEdit.error = null
        binding.textFieldLayoutCustomerAddressEdit.error = null
        binding.textFieldLayoutCustomerJobFieldEdit.error = null // Added
        binding.textFieldLayoutCustomerCompanyNameEdit.error = null // Added
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
