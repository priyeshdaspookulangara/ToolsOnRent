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
    private var selectedTool: Tool? = null
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
        setupToolSelection()
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
                        binding.textFieldLayoutCustomerStartRental.error = null // Clear error on selection
                    }
                }
            }
        }
        // Clear selection if text changes and doesn't match a valid customer
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

    private fun setupToolSelection() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableTools.collect { tools ->
                    // Display tool name and its price in the dropdown for clarity
                    val toolDisplayList = tools.map { "${it.name} - $${String.format("%.2f", it.rentalPrice)}/day" }
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, toolDisplayList)
                    binding.autoCompleteToolStartRental.setAdapter(adapter)
                    binding.autoCompleteToolStartRental.setOnItemClickListener { _, _, position, _ ->
                        selectedTool = tools[position]
                        binding.textViewSelectedToolPriceStartRental.text = "Price per day: $${String.format("%.2f", selectedTool?.rentalPrice)}"
                        // Set only the name in the text field after selection for cleaner UI
                        binding.autoCompleteToolStartRental.setText(selectedTool?.name, false)
                        binding.textFieldLayoutToolStartRental.error = null // Clear error on selection
                    }
                }
            }
        }
        // Clear selection and price if text changes and doesn't match a valid tool
        binding.autoCompleteToolStartRental.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val currentText = binding.autoCompleteToolStartRental.text.toString()
                 if (selectedTool?.name != currentText) {
                    binding.autoCompleteToolStartRental.setText("", false)
                    selectedTool = null
                    binding.textViewSelectedToolPriceStartRental.text = "Price per day: -"
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

            viewModel.confirmRental(selectedCustomer, selectedTool, rentalDate, dueDate, notes)
        }
    }

    private fun observeSaveResult() {
        viewModel.saveRentalResult.observe(viewLifecycleOwner) { result ->
            result.fold(
                onSuccess = {
                    Toast.makeText(requireContext(), "Rental confirmed successfully!", Toast.LENGTH_SHORT).show()
                    clearForm()
                    // findNavController().popBackStack() // Optionally navigate back
                },
                onFailure = { exception ->
                    Log.e("StartRentalFragment", "Error confirming rental", exception)
                    handleSaveError(exception)
                }
            )
        }
    }

    private fun handleSaveError(exception: Throwable) {
        val message = exception.message ?: "Unknown error occurred."
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

        // More specific error handling based on exception message from ViewModel
        when {
            message.contains("select a customer") -> binding.textFieldLayoutCustomerStartRental.error = "Required"
            message.contains("select a tool") -> binding.textFieldLayoutToolStartRental.error = "Required"
            message.contains("select a rental date") -> binding.textFieldLayoutRentalDateStartRental.error = "Required"
            message.contains("select a due date") -> binding.textFieldLayoutDueDateStartRental.error = "Required"
            message.contains("Due date cannot be before rental date") -> binding.textFieldLayoutDueDateStartRental.error = message
            message.contains("tool is no longer available") -> {
                binding.textFieldLayoutToolStartRental.error = "Tool unavailable"
                // Optionally refresh the tool list or clear selection
                selectedTool = null
                binding.autoCompleteToolStartRental.setText("", false)
                binding.textViewSelectedToolPriceStartRental.text = "Price per day: -"
            }
        }
    }

    private fun clearAllErrors() {
        binding.textFieldLayoutCustomerStartRental.error = null
        binding.textFieldLayoutToolStartRental.error = null
        binding.textFieldLayoutRentalDateStartRental.error = null
        binding.textFieldLayoutDueDateStartRental.error = null
    }

    private fun clearForm() {
        clearAllErrors()
        binding.autoCompleteCustomerStartRental.setText("", false)
        binding.autoCompleteToolStartRental.setText("", false)
        selectedCustomer = null
        selectedTool = null

        // Reset rental date to today, clear due date
        val todayCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        rentalDate = todayCalendar.time
        binding.editTextRentalDateStartRental.setText(dateFormat.format(rentalDate!!))

        dueDate = null
        binding.editTextDueDateStartRental.setText("")

        binding.textViewSelectedToolPriceStartRental.text = "Price per day: -"
        binding.editTextRentalNotesStartRental.text?.clear()

        binding.autoCompleteCustomerStartRental.requestFocus() // Set focus to the first field
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
