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
        setupKpiCards() // New call

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

    private fun setupKpiCards() {
        // Due Tool Returns Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dueToolReturnsCount.collectLatest { count ->
                    binding.includeDueToolReturnsCard.textViewKpiCount.text = count.toString()
                }
            }
        }
        binding.includeDueToolReturnsCard.textViewKpiLabel.text = getString(R.string.kpi_label_due_returns)
        binding.includeDueToolReturnsCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_due_alert_24)
        binding.includeDueToolReturnsCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_due_background_pale_red)
        )
        binding.includeDueToolReturnsCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_overdueRentalsReportFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_due_returns_report), e as IllegalArgumentException)
            }
        }

        // Total Completed Transactions Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.totalCompletedTransactionsCount.collectLatest { count ->
                    binding.includeTotalTransactionsCard.textViewKpiCount.text = count.toString()
                }
            }
        }
        binding.includeTotalTransactionsCard.textViewKpiLabel.text = getString(R.string.kpi_label_completed_txns)
        binding.includeTotalTransactionsCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_transactions_24)
        binding.includeTotalTransactionsCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_info_background_pale_blue)
        )
        binding.includeTotalTransactionsCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_nav_dashboard_to_allCompletedTransactionsFragment)
            } catch (e: Exception) {
                 handleNavError(getString(R.string.kpi_nav_error_total_txns_report), e as IllegalArgumentException)
            }
        }

        // Available Tools Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.availableToolsCount.collectLatest { count ->
                    binding.includeAvailableToolsCard.textViewKpiCount.text = count.toString()
                }
            }
        }
        binding.includeAvailableToolsCard.textViewKpiLabel.text = getString(R.string.kpi_label_available_tools)
        binding.includeAvailableToolsCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_available_tools_24) // Placeholder - define this
        binding.includeAvailableToolsCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_success_background_pale_green) // Placeholder - define this
        )
        binding.includeAvailableToolsCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_toolListFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_available_tools), e as IllegalArgumentException) // Placeholder
            }
        }

        // Rented Tools Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rentedToolsCount.collectLatest { count ->
                    binding.includeRentedToolsCard.textViewKpiCount.text = count.toString()
                }
            }
        }
        binding.includeRentedToolsCard.textViewKpiLabel.text = getString(R.string.kpi_label_rented_tools)
        binding.includeRentedToolsCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_rented_tools_24) // Placeholder - define this
        binding.includeRentedToolsCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_warning_background_pale_orange) // Placeholder - define this
        )
         binding.includeRentedToolsCard.kpiCardViewRoot.setOnClickListener {
            try {
                // Consider navigating to a screen showing only rented tools, or active rentals.
                // For now, let's use active rentals as it's closely related.
                findNavController().navigate(R.id.action_dashboardFragment_to_activeRentalsFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_rented_tools), e as IllegalArgumentException) // Placeholder
            }
        }

        // Overdue Tools Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.overdueToolsCount.collectLatest { count ->
                    binding.includeOverdueToolsCard.textViewKpiCount.text = count.toString()
                }
            }
        }
        binding.includeOverdueToolsCard.textViewKpiLabel.text = getString(R.string.kpi_label_overdue_tools)
        binding.includeOverdueToolsCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_overdue_tools_24) // Placeholder - define this
        binding.includeOverdueToolsCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_danger_background_pale_red) // Placeholder - define this
        )
        binding.includeOverdueToolsCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_overdueRentalsReportFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_overdue_tools), e as IllegalArgumentException) // Placeholder
            }
        }

        // Daily Profit Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dailyProfit.collectLatest { profit ->
                    binding.includeDailyProfitCard.textViewKpiCount.text = currencyFormat.format(profit)
                }
            }
        }
        binding.includeDailyProfitCard.textViewKpiLabel.text = getString(R.string.kpi_label_daily_profit)
        binding.includeDailyProfitCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_daily_profit_24) // Placeholder - define this
        binding.includeDailyProfitCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_success_background_pale_green) // Placeholder - define this
        )
        // Optional: Navigation for profit cards can go to profit/loss report
        binding.includeDailyProfitCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_profitLossReportFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_daily_profit), e as IllegalArgumentException) // Placeholder
            }
        }


        // Monthly Profit Card
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.monthlyProfit.collectLatest { profit ->
                    binding.includeMonthlyProfitCard.textViewKpiCount.text = currencyFormat.format(profit)
                }
            }
        }
        binding.includeMonthlyProfitCard.textViewKpiLabel.text = getString(R.string.kpi_label_monthly_profit)
        binding.includeMonthlyProfitCard.imageViewKpiIcon.setImageResource(R.drawable.ic_kpi_monthly_profit_24) // Placeholder - define this
        binding.includeMonthlyProfitCard.kpiCardViewRoot.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.kpi_info_background_pale_blue) // Placeholder - define this
        )
        binding.includeMonthlyProfitCard.kpiCardViewRoot.setOnClickListener {
            try {
                findNavController().navigate(R.id.action_dashboardFragment_to_profitLossReportFragment)
            } catch (e: Exception) {
                handleNavError(getString(R.string.kpi_nav_error_monthly_profit), e as IllegalArgumentException) // Placeholder
            }
        }
    }

    private fun observeDashboardMetrics() {
        // This function is now effectively replaced by the individual collectors in setupKpiCards()
        // and the direct observation for non-KPI metrics if any were kept.
        // If all metrics are now KPIs, this function can be removed or left empty.
        // For now, let's clear it out as the metrics it observed are now handled by setupKpiCards.
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
