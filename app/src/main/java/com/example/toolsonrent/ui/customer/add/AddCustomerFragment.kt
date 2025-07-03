package com.example.toolsonrent.ui.customer.add

package com.example.toolsonrent.ui.customer.add

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.toolsonrent.R
import com.example.toolsonrent.databinding.FragmentAddCustomerBinding
import com.example.toolsonrent.databinding.ItemPhoneEntryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AddCustomerFragment : Fragment() {

    private var _binding: FragmentAddCustomerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddCustomerViewModel
    private val phoneTypes = arrayOf("Mobile", "Work", "Home", "Other") // Define phone types

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCustomerBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddCustomerViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDynamicPhoneEntries()
        setupReferrerSelection()

        binding.buttonAddPhoneNumber.setOnClickListener {
            viewModel.addPhoneNumberField()
        }

        binding.buttonSaveCustomer.setOnClickListener {
            collectDataAndSaveCustomer()
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { customerId ->
                    Toast.makeText(requireContext(), "Customer saved successfully! ID: $customerId", Toast.LENGTH_SHORT).show()
                    clearForm()
                    // findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("AddCustomerFragment", "Error saving customer", exception)
                    Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                    // TODO: More specific error highlighting based on ViewModel's state or exception type
                    if (binding.editTextCustomerName.text.isNullOrBlank()) {
                         binding.textFieldLayoutCustomerName.error = "Name cannot be empty"
                    }
                }
            )
        }

        // Observe ViewModel state for referrer name
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedReferrerId.collectLatest { referrerId ->
                if (referrerId != null) {
                    // Find the referrer name from the potentialReferrers list
                    val referrer = viewModel.potentialReferrers.value.find { it.id == referrerId }
                    binding.textViewSelectedReferrer.text = "Selected Referrer: ${referrer?.name ?: "Unknown"}"
                } else {
                    binding.textViewSelectedReferrer.text = "Selected Referrer: None"
                }
            }
        }
    }

    private fun setupDynamicPhoneEntries() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.phoneNumbers.collectLatest { phoneList ->
                binding.phoneNumbersContainer.removeAllViews() // Clear existing views
                phoneList.forEachIndexed { index, tempPhone ->
                    val phoneEntryBinding = ItemPhoneEntryBinding.inflate(layoutInflater, binding.phoneNumbersContainer, false)

                    phoneEntryBinding.editTextPhoneNumberItem.setText(tempPhone.number)
                    // Setup Spinner
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, phoneTypes)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    phoneEntryBinding.spinnerPhoneTypeItem.adapter = adapter
                    val typePosition = phoneTypes.indexOf(tempPhone.type).coerceAtLeast(0)
                    phoneEntryBinding.spinnerPhoneTypeItem.setSelection(typePosition)

                    // Listeners to update ViewModel
                    phoneEntryBinding.editTextPhoneNumberItem.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            viewModel.updatePhoneNumberValue(index, phoneEntryBinding.editTextPhoneNumberItem.text.toString())
                        }
                    }
                    // TODO: Add TextWatcher for more immediate updates if needed for editTextPhoneNumberItem

                    phoneEntryBinding.spinnerPhoneTypeItem.onItemSelectedListener =
                        object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                                viewModel.updatePhoneType(index, phoneTypes[position])
                            }
                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }

                    phoneEntryBinding.buttonRemovePhoneItem.setOnClickListener {
                        viewModel.removePhoneNumberField(tempPhone)
                    }
                    binding.phoneNumbersContainer.addView(phoneEntryBinding.root)
                }
            }
        }
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
                    val selected = referrers[which]
                    viewModel.selectedReferrerId.value = selected.id
                    // textViewSelectedReferrer will update via observer
                    dialog.dismiss()
                }
                .setNegativeButton("Clear Referrer") {dialog, _ ->
                    viewModel.selectedReferrerId.value = null
                    dialog.dismiss()
                }
                .setNeutralButton("Cancel", null)
                .show()
        }
    }


    private fun collectDataAndSaveCustomer() {
        // Update ViewModel states from UI before saving, especially for EditTexts that update on focus loss
        binding.editTextCustomerName.text.toString().trim().let { viewModel.name.value = it }
        binding.editTextCustomerEmail.text.toString().trim().let { viewModel.email.value = it }
        binding.editTextCustomerAddress.text.toString().trim().let { viewModel.address.value = it }
        binding.editTextCustomerJobField.text.toString().trim().let { viewModel.jobField.value = it }
        binding.editTextCustomerCompanyName.text.toString().trim().let { viewModel.companyName.value = it }

        // Ensure phone numbers are also up-to-date from their EditTexts
        for (i in 0 until binding.phoneNumbersContainer.childCount) {
            val phoneEntryView = binding.phoneNumbersContainer.getChildAt(i)
            val editText = phoneEntryView.findViewById<EditText>(R.id.editTextPhoneNumberItem) // Assuming R.id from item_phone_entry
            // val spinner = phoneEntryView.findViewById<Spinner>(R.id.spinnerPhoneTypeItem) // Type already updated by its listener
            if (i < viewModel.phoneNumbers.value.size) { // Check bounds
                 viewModel.updatePhoneNumberValue(i, editText.text.toString())
            }
        }

        clearAllErrors() // Clear previous errors
        viewModel.saveCustomer()
    }

    private fun clearAllErrors() {
        binding.textFieldLayoutCustomerName.error = null
        binding.textFieldLayoutCustomerEmail.error = null
        binding.textFieldLayoutCustomerAddress.error = null
        binding.textFieldLayoutCustomerJobField.error = null
        binding.textFieldLayoutCustomerCompanyName.error = null
        // Errors on dynamic phone fields would need specific handling if added
    }


    private fun clearForm() {
        viewModel.name.value = ""
        viewModel.email.value = null
        viewModel.address.value = null
        viewModel.jobField.value = null
        viewModel.companyName.value = null
        viewModel.selectedReferrerId.value = null
        // Reset phone numbers in ViewModel, which will trigger UI update via collectLatest
        viewModel.addPhoneNumberField() // This will replace existing with one empty or define a clear function in VM

        binding.editTextCustomerName.text?.clear()
        binding.editTextCustomerEmail.text?.clear()
        binding.editTextCustomerAddress.text?.clear()
        binding.editTextCustomerJobField.text?.clear()
        binding.editTextCustomerCompanyName.text?.clear()
        binding.textViewSelectedReferrer.text = "Selected Referrer: None"
        // phoneNumbersContainer will be cleared and repopulated by the observer

        clearAllErrors()
        binding.editTextCustomerName.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
