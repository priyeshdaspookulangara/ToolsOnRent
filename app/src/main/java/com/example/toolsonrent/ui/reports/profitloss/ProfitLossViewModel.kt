package com.example.toolsonrent.ui.reports.profitloss

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.RentalTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest
class ProfitLossViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()

    private val _selectedStartDateFlow = MutableStateFlow<Date?>(null)
    val selectedStartDate: StateFlow<Date?> = _selectedStartDateFlow.asStateFlow()

    private val _selectedEndDateFlow = MutableStateFlow<Date?>(null)
    val selectedEndDate: StateFlow<Date?> = _selectedEndDateFlow.asStateFlow()

    fun updateSelectedDates(start: Date?, end: Date?) {
        val calendar = Calendar.getInstance()

        _selectedStartDateFlow.value = start?.let {
            calendar.time = it
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
        }

        _selectedEndDateFlow.value = end?.let {
            calendar.time = it
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            calendar.time
        }
        Log.d("ProfitLossVM", "Dates updated: Start=${_selectedStartDateFlow.value}, End=${_selectedEndDateFlow.value}")
    }

    init {
        // Set initial date range: from the first day of the current month to the current day.
        val calendar = Calendar.getInstance()
        // Set to the first day of the current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val initialStartDate = calendar.time

        // Current date (will be adjusted to end of day by updateSelectedDates)
        val initialEndDate = Date()

        updateSelectedDates(initialStartDate, initialEndDate)
    }

    // StateFlow that calculates total profit based on selected date range.
    // It combines the latest start and end dates, then uses flatMapLatest to switch
    // to the new database query result flow when dates change.
    val calculatedProfit: StateFlow<Double> = combine(
        _selectedStartDateFlow,
        _selectedEndDateFlow
    ) { startDate, endDate ->
        // This Pair is just to make combine happy and trigger flatMapLatest with both dates.
        Pair(startDate, endDate)
    }.flatMapLatest { (startDate, endDate) ->
        if (startDate != null && endDate != null && !endDate.before(startDate)) {
            // If dates are valid, fetch completed transactions within this range.
            transactionDao.getCompletedTransactionsInRange(startDate.time, endDate.time)
                .map { transactions ->
                    // Calculate total profit from the fetched transactions.
                    calculateTotalProfit(transactions)
                }
        } else {
            // If dates are invalid or not set, emit 0.0 profit.
            flowOf(0.0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0.0 // Initial profit is 0.0 before calculation.
    )

    private fun calculateTotalProfit(transactions: List<RentalTransaction>): Double {
        return transactions.sumOf { transaction ->
            // Ensure there's a return date; otherwise, it's not a completed transaction for profit calculation.
            // The DAO query should already ensure returnDate is not null, but this is a safeguard.
            if (transaction.returnDate == null) {
                Log.w("ProfitCalc", "Transaction ${transaction.id} included in profit calculation but has no return date.")
                return@sumOf 0.0
            }

            // Business Rule for Duration:
            // A rental period spanning any part of a day incurs a charge for that full day.
            // Minimum 1 day charge for any completed rental.
            val diffMillis = transaction.returnDate!!.time - transaction.rentalDate.time

            // Calculate days by dividing milliseconds by ms_in_day.
            // Add a small epsilon to handle potential floating point inaccuracies if we were using floating point division.
            // For integer division of ms, this is not strictly needed but doesn't hurt.
            // The core idea is to count the number of 24-hour blocks.
            var durationInDays = TimeUnit.MILLISECONDS.toDays(diffMillis)

            // If the rental started and ended on the same calendar day, it's 1 day.
            // If it's 0 days by pure calculation (e.g. less than 24h), count as 1 day.
            if (durationInDays == 0L) {
                 durationInDays = 1L
            } else {
                // If it spans across multiple days, check if there's any remainder time.
                // If rental from Mon 10 AM to Tue 11 AM, toDays gives 1. This is 2 days charge.
                // So, if there's any part of the next day, add 1.
                // A robust way for "days charged" or "number of midnights passed + 1"
                val calRental = Calendar.getInstance().apply { time = transaction.rentalDate; clearTime() }
                val calReturn = Calendar.getInstance().apply { time = transaction.returnDate!!; clearTime() }

                durationInDays = TimeUnit.MILLISECONDS.toDays(calReturn.timeInMillis - calRental.timeInMillis) + 1
            }

            (durationInDays * transaction.rentalPricePerDay).coerceAtLeast(0.0)
        }
    }

    // Helper to clear time components from a Calendar instance
    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
