package com.example.toolsonrent.ui.reports.profitloss

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.toolsonrent.databinding.FragmentProfitLossReportBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone // For MaterialDatePicker which uses UTC

class ProfitLossReportFragment : Fragment() {

    private var _binding: FragmentProfitLossReportBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfitLossViewModel
    // Date formatter for displaying dates in EditText fields and the range text.
    private val displayDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    // Currency formatter for displaying the profit value.
    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfitLossReportBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ProfitLossReportViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePickers()
        observeViewModelData()
    }

    private fun setupDatePickers() {
        // Set click listeners for both the EditText and the end icon of TextInputLayout
        binding.editTextStartDateProfit.setOnClickListener { showDatePickerDialog(isStartDate = true) }
        binding.textFieldLayoutStartDateProfit.setEndIconOnClickListener { showDatePickerDialog(isStartDate = true) }

        binding.editTextEndDateProfit.setOnClickListener { showDatePickerDialog(isStartDate = false) }
        binding.textFieldLayoutEndDateProfit.setEndIconOnClickListener { showDatePickerDialog(isStartDate = false) }
    }

    private fun showDatePickerDialog(isStartDate: Boolean) {
        val todayUtc = MaterialDatePicker.todayInUtcMilliseconds()

        val currentSelection = if (isStartDate) {
            viewModel.selectedStartDate.value?.time ?: todayUtc
        } else {
            viewModel.selectedEndDate.value?.time ?: viewModel.selectedStartDate.value?.time ?: todayUtc
        }

        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText(if (isStartDate) "Select Start Date" else "Select End Date")
            .setSelection(currentSelection)

        // Add constraints to the date picker
        val constraintsBuilder = CalendarConstraints.Builder()
        if (!isStartDate) { // When picking End Date
            viewModel.selectedStartDate.value?.let { startDate ->
                // End date must be on or after the selected start date.
                constraintsBuilder.setValidator(DateValidatorPointForward.from(startDate.time))
            }
        }
        // For start date, no specific constraint other than it cannot be after the end date,
        // which is implicitly handled by how the end date picker is constrained if start date is picked first.
        // We could also add a constraint to not pick future dates if desired, but not in plan for now.

        builder.setCalendarConstraints(constraintsBuilder.build())
        val datePicker = builder.build()

        datePicker.addOnPositiveButtonClickListener { selectedTimestamp ->
            // MaterialDatePicker returns timestamp in UTC.
            // Creating a new Date object from this UTC timestamp is correct.
            val selectedDate = Date(selectedTimestamp)
            if (isStartDate) {
                // If selected start date is after current end date, clear end date or adjust.
                val currentEndDate = viewModel.selectedEndDate.value
                if (currentEndDate != null && selectedDate.after(currentEndDate)) {
                    viewModel.updateSelectedDates(selectedDate, null) // Clear end date
                } else {
                    viewModel.updateSelectedDates(selectedDate, currentEndDate)
                }
            } else { // Picking End Date
                viewModel.updateSelectedDates(viewModel.selectedStartDate.value, selectedDate)
            }
        }
        datePicker.show(childFragmentManager, datePicker.toString())
    }

    private fun observeViewModelData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe Start Date
                launch {
                    viewModel.selectedStartDate.collectLatest { date ->
                        binding.editTextStartDateProfit.setText(date?.let { displayDateFormat.format(it) } ?: "")
                        updateDateRangeText()
                    }
                }

                // Observe End Date
                launch {
                    viewModel.selectedEndDate.collectLatest { date ->
                        binding.editTextEndDateProfit.setText(date?.let { displayDateFormat.format(it) } ?: "")
                        updateDateRangeText()
                    }
                }

                // Observe Calculated Profit
                launch {
                    viewModel.calculatedProfit.collectLatest { profit ->
                        binding.textViewTotalProfitValue.text = currencyFormat.format(profit)
                    }
                }
            }
        }
    }

    private fun updateDateRangeText() {
        val startDate = viewModel.selectedStartDate.value
        val endDate = viewModel.selectedEndDate.value

        val startDateStr = startDate?.let { displayDateFormat.format(it) } ?: "N/A"
        val endDateStr = endDate?.let { displayDateFormat.format(it) } ?: "N/A"

        if (startDate != null && endDate != null) {
            binding.textViewDateRangeProfit.text = "Selected Range: $startDateStr - $endDateStr"
        } else if (startDate != null) {
            binding.textViewDateRangeProfit.text = "Selected Range: From $startDateStr"
        } else if (endDate != null) {
            binding.textViewDateRangeProfit.text = "Selected Range: Up to $endDateStr"
        } else {
            binding.textViewDateRangeProfit.text = "Selected Range: N/A - N/A"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
