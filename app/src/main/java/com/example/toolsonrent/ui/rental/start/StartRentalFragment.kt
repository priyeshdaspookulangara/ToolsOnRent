package com.example.toolsonrent.ui.rental.start

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.toolsonrent.R
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.databinding.FragmentStartRentalBinding // Generated
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class StartRentalFragment : Fragment() {

    private var _binding: FragmentStartRentalBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StartRentalViewModel

    private var selectedCustomer: Customer? = null
    private var selectedToolType: Tool? = null // Renamed from selectedTool
    private var selectedToolInstance: ToolInstance? = null // Added
    private var rentalDate: Date? = null
    private var dueDate: Date? = null

    // Date formatter for displaying dates in EditText fields
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartRentalBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[StartRentalViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCustomerSelection()
        setupToolTypeSelection() // Renamed from setupToolSelection
        setupToolInstanceSelection() // Added
        setupDatePickers()
        setupConfirmButton()
        observeSaveResult()
    }

    private fun setupCustomerSelection() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allCustomers.collect { customers ->
                    val customerNames = customers.map { it.name }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, customerNames)
                    binding.autoCompleteCustomerStartRental.setAdapter(adapter)
                    binding.autoCompleteCustomerStartRental.setOnItemClickListener { _, _, position, _ ->
                        selectedCustomer = customers[position]
                        binding.textFieldLayoutCustomerStartRental.error = null
                    }
                }
            }
        }
        binding.autoCompleteCustomerStartRental.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentText = binding.autoCompleteCustomerStartRental.text.toString()
                if (selectedCustomer?.name != currentText) {
                    binding.autoCompleteCustomerStartRental.setText("", false)
                    selectedCustomer = null
                }
            }
        }
    }

    private fun setupToolTypeSelection() { // Renamed
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allToolTypes.collect { toolTypes -> // Changed to allToolTypes
                    val toolDisplayList = toolTypes.map { "${it.name} - $${String.format(Locale.US, "%.2f", it.rentalPrice)}/day" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, toolDisplayList)
                    binding.autoCompleteToolStartRental.setAdapter(adapter)
                    binding.autoCompleteToolStartRental.setOnItemClickListener { _, _, position, _ ->
                        selectedToolType = toolTypes[position] // Changed
                        binding.textViewSelectedToolPriceStartRental.text = getString(R.string.price_per_day_dynamic, String.format(Locale.US, "%.2f", selectedToolType?.rentalPrice))
                        binding.autoCompleteToolStartRental.setText(selectedToolType?.name, false)
                        binding.textFieldLayoutToolStartRental.error = null

                        // Trigger loading of instances for this type
                        viewModel.setSelectedToolType(selectedToolType?.id)
                        binding.textFieldLayoutToolInstanceStartRental.visibility = View.VISIBLE
                        binding.autoCompleteToolInstanceStartRental.setText("", false) // Clear previous instance selection
                        selectedToolInstance = null
                        binding.textFieldLayoutToolInstanceStartRental.error = null
                    }
                }
            }
        }
        binding.autoCompleteToolStartRental.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentText = binding.autoCompleteToolStartRental.text.toString()
                 if (selectedToolType?.name != currentText) { // Changed
                    binding.autoCompleteToolStartRental.setText("", false)
                    selectedToolType = null
                    selectedToolInstance = null
                    binding.textViewSelectedToolPriceStartRental.text = getString(R.string.price_per_day_default)
                    viewModel.setSelectedToolType(null) // Clear selected type in VM
                    binding.textFieldLayoutToolInstanceStartRental.visibility = View.GONE
                    binding.autoCompleteToolInstanceStartRental.setText("", false)
                 }
            }
        }
    }

    private fun setupToolInstanceSelection() { // Added
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableToolInstances.collect { instances ->
                    val instanceDisplayList = instances.map {
                        if (!it.serialNumber.isNullOrBlank()) "SN: ${it.serialNumber}" else "ID: ${it.instanceId}"
                    }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, instanceDisplayList)
                    binding.autoCompleteToolInstanceStartRental.setAdapter(adapter)
                    binding.autoCompleteToolInstanceStartRental.setOnItemClickListener { _, _, position, _ ->
                        selectedToolInstance = instances[position]
                        // Optionally, set the display text more explicitly if needed
                        binding.autoCompleteToolInstanceStartRental.setText(instanceDisplayList[position], false)
                        binding.textFieldLayoutToolInstanceStartRental.error = null
                    }
                    // If the selected tool type has no available instances, inform the user
                    if (binding.textFieldLayoutToolInstanceStartRental.visibility == View.VISIBLE && instances.isEmpty()) {
                        binding.textFieldLayoutToolInstanceStartRental.error = "No specific items available for this tool type."
                        // Consider disabling the field or showing a more prominent message.
                    } else if (binding.textFieldLayoutToolInstanceStartRental.visibility == View.VISIBLE) {
                         binding.textFieldLayoutToolInstanceStartRental.error = null // Clear error if instances become available
                    }
                }
            }
        }
        binding.autoCompleteToolInstanceStartRental.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentText = binding.autoCompleteToolInstanceStartRental.text.toString()
                val currentInstanceDisplay = if (selectedToolInstance?.serialNumber.isNullOrBlank()) "ID: ${selectedToolInstance?.instanceId}" else "SN: ${selectedToolInstance?.serialNumber}"
                if (selectedToolInstance != null && currentInstanceDisplay != currentText) {
                    binding.autoCompleteToolInstanceStartRental.setText("", false)
                    selectedToolInstance = null
                }
            }
        }
    }

    private fun setupDatePickers() {
        // Default rental date to today
        val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")) // Use UTC for MaterialDatePicker
        rentalDate = todayCalendar.time
        binding.editTextRentalDateStartRental.setText(dateFormat.format(rentalDate!!))

        binding.editTextRentalDateStartRental.setOnClickListener { showDatePickerDialog(isRentalDate = true) }
        binding.textFieldLayoutRentalDateStartRental.setEndIconOnClickListener { showDatePickerDialog(isRentalDate = true) }

        binding.editTextDueDateStartRental.setOnClickListener { showDatePickerDialog(isRentalDate = false) }
        binding.textFieldLayoutDueDateStartRental.setEndIconOnClickListener { showDatePickerDialog(isRentalDate = false) }
    }

    private fun showDatePickerDialog(isRentalDate: Boolean) {
        val todayUtc = MaterialDatePicker.todayInUtcMilliseconds()
        val currentSelection = when {
            isRentalDate && rentalDate != null -> rentalDate!!.time
            !isRentalDate && dueDate != null -> dueDate!!.time
            else -> todayUtc
        }

        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isRentalDate) "Select Rental Date" else "Select Due Date")
            .setSelection(currentSelection)

        val constraintsBuilder = CalendarConstraints.Builder()
        if (isRentalDate) {
            // Rental date can be today or future
            constraintsBuilder.setValidator(DateValidatorPointForward.from(todayUtc))
        } else {
            // Due date must be after or on rental date (if rental date is set)
            // Or today if rental date is not set yet (though we default rental date)
            val minDueDate = rentalDate?.time ?: todayUtc
            constraintsBuilder.setValidator(DateValidatorPointForward.from(minDueDate))
        }
        datePickerBuilder.setCalendarConstraints(constraintsBuilder.build())

        val datePicker = datePickerBuilder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Date(selection) // Selection is in UTC milliseconds
            if (isRentalDate) {
                rentalDate = selectedDate
                binding.editTextRentalDateStartRental.setText(dateFormat.format(selectedDate))
                binding.textFieldLayoutRentalDateStartRental.error = null // Clear error
                // If due date was before new rental date, or if due date is null, suggest new due date or clear
                if (dueDate != null && dueDate!!.before(rentalDate)) {
                    dueDate = null
                    binding.editTextDueDateStartRental.setText("")
                    Toast.makeText(requireContext(), "Due date cleared as it was before new rental date.", Toast.LENGTH_SHORT).show()
                }
            } else {
                dueDate = selectedDate
                binding.editTextDueDateStartRental.setText(dateFormat.format(selectedDate))
                binding.textFieldLayoutDueDateStartRental.error = null // Clear error
            }
        }
        datePicker.show(childFragmentManager, datePicker.toString())
    }

    private fun setupConfirmButton() {
        binding.buttonConfirmRental.setOnClickListener {
            val notes = binding.editTextRentalNotesStartRental.text.toString().trim()

            // Clear previous errors first
            clearAllErrors()

            viewModel.confirmRental(
                selectedCustomer,
                selectedToolInstance, // Pass selected instance
                rentalDate,
                dueDate,
                notes,
                selectedToolType?.rentalPrice // Pass rental price from the tool type
            )
        }
    }

    private fun observeSaveResult() {
        viewModel.saveRentalResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = { transactionId -> // Now receives transactionId
                    Toast.makeText(requireContext(), getString(R.string.rental_confirmed_success_id, transactionId), Toast.LENGTH_LONG).show()
                    clearForm()
                    // findNavController().popBackStack()
                },
                onFailure = { exception ->
                    Log.e("StartRentalFragment", "Error confirming rental", exception)
                    handleSaveError(exception)
                }
            )
        }
    }

    private fun handleSaveError(exception: Throwable) {
        val message = exception.message ?: getString(R.string.unknown_error_occurred)
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        when {
            message.contains("select a customer", ignoreCase = true) -> binding.textFieldLayoutCustomerStartRental.error = getString(R.string.error_field_required)
            message.contains("select a tool type", ignoreCase = true) -> binding.textFieldLayoutToolStartRental.error = getString(R.string.error_field_required) // For tool type
            message.contains("select a specific item", ignoreCase = true) -> binding.textFieldLayoutToolInstanceStartRental.error = getString(R.string.error_field_required) // For instance
            message.contains("select a rental date", ignoreCase = true) -> binding.textFieldLayoutRentalDateStartRental.error = getString(R.string.error_field_required)
            message.contains("select a due date", ignoreCase = true) -> binding.textFieldLayoutDueDateStartRental.error = getString(R.string.error_field_required)
            message.contains("Due date cannot be before rental date", ignoreCase = true) -> binding.textFieldLayoutDueDateStartRental.error = message
            message.contains("item is no longer available", ignoreCase = true) -> {
                binding.textFieldLayoutToolInstanceStartRental.error = "Item unavailable"
                // Clear instance selection and refresh instances
                selectedToolInstance = null
                binding.autoCompleteToolInstanceStartRental.setText("", false)
                viewModel.setSelectedToolType(selectedToolType?.id) // Re-trigger instance fetch
            }
             message.contains("Invalid rental price", ignoreCase = true) -> {
                // This error is less likely to be triggered by user directly if price comes from selectedToolType
                Toast.makeText(requireContext(), "Error with rental price. Please re-select tool.", Toast.LENGTH_LONG).show()
                binding.textFieldLayoutToolStartRental.error = "Re-select tool"
            }
        }
    }

    private fun clearAllErrors() {
        binding.textFieldLayoutCustomerStartRental.error = null
        binding.textFieldLayoutToolStartRental.error = null
        binding.textFieldLayoutToolInstanceStartRental.error = null // Added
        binding.textFieldLayoutRentalDateStartRental.error = null
        binding.textFieldLayoutDueDateStartRental.error = null
    }

    private fun clearForm() {
        clearAllErrors()
        binding.autoCompleteCustomerStartRental.setText("", false)
        binding.autoCompleteToolStartRental.setText("", false)
        binding.autoCompleteToolInstanceStartRental.setText("", false) // Added
        selectedCustomer = null
        selectedToolType = null // Changed
        selectedToolInstance = null // Added
        binding.textFieldLayoutToolInstanceStartRental.visibility = View.GONE // Added

        val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        rentalDate = todayCalendar.time
        binding.editTextRentalDateStartRental.setText(dateFormat.format(rentalDate!!))

        dueDate = null
        binding.editTextDueDateStartRental.setText("")

        binding.textViewSelectedToolPriceStartRental.text = getString(R.string.price_per_day_default)
        binding.editTextRentalNotesStartRental.text?.clear()
        viewModel.setSelectedToolType(null) // Clear selection in VM

        binding.autoCompleteCustomerStartRental.requestFocus()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
