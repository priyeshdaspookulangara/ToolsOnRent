package com.example.toolsonrent.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.toolsonrent.R
import kotlinx.coroutines.launch
import com.example.toolsonrent.databinding.FragmentDashboardBinding
import com.example.toolsonrent.ui.dashboard.calendar.EventDecorator
import com.example.toolsonrent.ui.dashboard.calendar.details.DailyRentalsBottomSheetDialogFragment // New import
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DashboardViewModel

    private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        maximumFractionDigits = 2
    }

    private var dueTodayDecorator: EventDecorator? = null
    private var overdueDecorator: EventDecorator? = null
    private var upcomingDecorator: EventDecorator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setupNavigationButtonListeners()
        observeDashboardMetrics()
        observeCalendarDecorators()

        // Updated MaterialCalendarView listener
        binding.materialCalendarDashboard.setOnDateChangedListener { widget, date, selected ->
            if (selected) { // User selected a date
                viewModel.userSelectedDateForDetails(date) // Inform ViewModel

                // Show the BottomSheetDialogFragment
                // Check if already shown to prevent multiple instances
                if (childFragmentManager.findFragmentByTag(DailyRentalsBottomSheetDialogFragment.TAG) == null) {
                    DailyRentalsBottomSheetDialogFragment.newInstance().show(
                        childFragmentManager,
                        DailyRentalsBottomSheetDialogFragment.TAG
                    )
                }
            } else {
                // Optional: If a date is deselected (and calendar supports this mode, though default is single selection replacement)
                // viewModel.userSelectedDateForDetails(null) // Clear details if deselected
            }
        }
    }

    private fun setupNavigationButtonListeners() {
        binding.buttonGoToToolList.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_toolListFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Manage Tools", e) }
        }
        binding.buttonManageCustomersDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_customerListFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Manage Customers", e) }
        }
        binding.buttonManageReturnsDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_activeRentalsFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Manage Returns", e) }
        }
        binding.buttonViewOverdueReportDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_overdueRentalsReportFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Overdue Report", e) }
        }
        binding.buttonCustomerHistoryReportDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_selectCustomerForReportFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Customer History Report", e) }
        }
        binding.buttonToolHistoryReportDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_selectToolForReportFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Tool History Report", e) }
        }
        binding.buttonInventoryReportDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_inventoryReportFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Inventory Report", e) }
        }
        binding.buttonProfitLossReportDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_profitLossReportFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Profit & Loss Report", e) }
        }
        binding.buttonAddNewToolDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_addToolFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Add New Tool", e) }
        }
        binding.buttonAddNewCustomerDashboard.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_addCustomerFragment)
            } catch (e: IllegalArgumentException) {
                handleNavError("Add New Customer", e)
            }
        }
        binding.buttonStartNewRentalDashboard.setOnClickListener {
            try { findNavController().navigate(R.id.action_dashboardFragment_to_startRentalFragment) }
            catch (e: IllegalArgumentException) { handleNavError("Start New Rental", e) }
        }
    }

    private fun handleNavError(actionName: String, exception: IllegalArgumentException) {
        Log.e("DashboardFragment", "Navigation for '$actionName' failed. Action not found.", exception)
        Toast.makeText(context, "Error: $actionName action not found.", Toast.LENGTH_SHORT).show()
    }

    private fun observeDashboardMetrics() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.availableToolsCount.collectLatest { count -> binding.textViewAvailableCountValue.text = count.toString() } }
                launch { viewModel.rentedToolsCount.collectLatest { count -> binding.textViewRentedCountValue.text = count.toString() } }
                launch { viewModel.overdueToolsCount.collectLatest { count -> binding.textViewOverdueCountValue.text = count.toString() } }
                launch { viewModel.dailyProfit.collectLatest { profit -> binding.textViewDailyProfitValue.text = currencyFormat.format(profit) } }
                launch { viewModel.monthlyProfit.collectLatest { profit -> binding.textViewMonthlyProfitValue.text = currencyFormat.format(profit) } }
            }
        }
    }

    private fun observeCalendarDecorators() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.dueTodayCalendarDays.collectLatest { dates ->
                        dueTodayDecorator?.let { binding.materialCalendarDashboard.removeDecorator(it) }
                        if (dates.isNotEmpty()) {
                            val color = ContextCompat.getColor(requireContext(), R.color.calendar_due_today_yellow)
                            dueTodayDecorator = EventDecorator(color, dates)
                            binding.materialCalendarDashboard.addDecorator(dueTodayDecorator!!)
                        } else { dueTodayDecorator = null }
                    }
                }
                launch {
                    viewModel.overdueUnreturnedCalendarDays.collectLatest { dates ->
                        overdueDecorator?.let { binding.materialCalendarDashboard.removeDecorator(it) }
                        if (dates.isNotEmpty()) {
                            val color = ContextCompat.getColor(requireContext(), R.color.status_rented_red)
                            overdueDecorator = EventDecorator(color, dates)
                            binding.materialCalendarDashboard.addDecorator(overdueDecorator!!)
                        } else { overdueDecorator = null }
                    }
                }
                launch {
                    viewModel.upcomingReturnCalendarDays.collectLatest { dates ->
                        upcomingDecorator?.let { binding.materialCalendarDashboard.removeDecorator(it) }
                        if (dates.isNotEmpty()) {
                            val color = ContextCompat.getColor(requireContext(), R.color.status_active_blue)
                            upcomingDecorator = EventDecorator(color, dates)
                            binding.materialCalendarDashboard.addDecorator(upcomingDecorator!!)
                        } else { upcomingDecorator = null }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
