package com.example.toolsonrent.ui.dashboard.calendar.details

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels // For sharing ViewModel with host Activity/Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
// Import DividerItemDecoration if you want to add dividers to the RecyclerView
// import androidx.recyclerview.widget.DividerItemDecoration
import com.example.toolsonrent.databinding.BottomSheetDailyRentalsBinding // Generated
import com.example.toolsonrent.ui.dashboard.DashboardViewModel // Shared ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.prolificinteractive.materialcalendarview.CalendarDay // For observing selectedCalendarDayForDetails
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
// java.util.Date is used by CalendarDay.getDate()

class DailyRentalsBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDailyRentalsBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Use activityViewModels() to get a ViewModel instance scoped to the host Activity.
    // This assumes DashboardFragment (which triggers this BottomSheet) and this BottomSheet
    // share the same host Activity, and DashboardViewModel is registered at Activity level or
    // through the DashboardFragment's host.
    private val viewModel: DashboardViewModel by activityViewModels()

    private lateinit var dailyRentalDetailAdapter: DailyRentalDetailAdapter
    // Date formatter for the title of the BottomSheet.
    private val titleDateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetDailyRentalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dailyRentalDetailAdapter = DailyRentalDetailAdapter()

        setupRecyclerView()
        observeSelectedDateAndTitle()
        observeDailyRentalDetails()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewDailyDetails.apply {
            adapter = dailyRentalDetailAdapter
            layoutManager = LinearLayoutManager(requireContext())
            // Example of adding a divider:
            // addItemDecoration(DividerItemDecoration(context, (layoutManager as LinearLayoutManager).orientation))
        }
    }

    private fun observeSelectedDateAndTitle() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedCalendarDayForDetails.collectLatest { calendarDay ->
                    if (calendarDay != null) {
                        // com.prolificinteractive.materialcalendarview.CalendarDay.getDate() returns java.util.Date
                        val date = calendarDay.date
                        binding.textViewBottomSheetTitleDaily.text = "Activity for ${titleDateFormat.format(date)}"
                    } else {
                        binding.textViewBottomSheetTitleDaily.text = "No Date Selected" // Or dismiss if no date?
                    }
                }
            }
        }
    }

    private fun observeDailyRentalDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rentalsForSelectedDate.collectLatest { detailsList ->
                    dailyRentalDetailAdapter.submitList(detailsList)
                    val listIsEmpty = detailsList.isEmpty()
                    binding.textViewNoRentalsForDate.isVisible = listIsEmpty
                    binding.recyclerViewDailyDetails.isVisible = !listIsEmpty
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // When the BottomSheet is dismissed, signal the ViewModel to clear the selected date.
        // This prevents the sheet from potentially re-showing the same details if it's opened again
        // without a new date selection, or on configuration changes.
        viewModel.userSelectedDateForDetails(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the adapter from RecyclerView to help prevent memory leaks.
        binding.recyclerViewDailyDetails.adapter = null
        _binding = null // Release the binding instance.
    }

    companion object {
        const val TAG = "DailyRentalsBottomSheet"
        // Factory method to create a new instance of this fragment.
        // Useful if you ever need to pass arguments directly to the BottomSheet for its creation,
        // though in this case, it relies on a shared ViewModel.
        fun newInstance(): DailyRentalsBottomSheetDialogFragment {
            return DailyRentalsBottomSheetDialogFragment()
        }
    }
}
