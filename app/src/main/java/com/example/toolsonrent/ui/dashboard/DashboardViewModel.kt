package com.example.toolsonrent.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.RentalTransaction // For helper function
import kotlinx.coroutines.flow.* // Import all from flow for map, flow, emitAll etc.
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao() // Added

    // Existing counts
    val availableToolsCount: StateFlow<Int> = toolDao.getAvailableToolsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val rentedToolsCount: StateFlow<Int> = toolDao.getRentedToolsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    // New overdue tools count
    val overdueToolsCount: StateFlow<Int> = rentalTransactionDao.getOverdueRentals(System.currentTimeMillis())
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    // Helper function to calculate profit from a list of transactions
    private fun calculateProfitFromTransactions(transactions: List<RentalTransaction>): Double {
        return transactions.sumOf { transaction ->
            if (transaction.returnDate == null) return@sumOf 0.0 // Only consider completed transactions

            // Business rule: minimum 1 day charge if any time passed and returned.
            // If rented and returned on the same calendar day, it's 1 day.
            val calRental = Calendar.getInstance().apply { time = transaction.rentalDate; clearTime() }
            val calReturn = Calendar.getInstance().apply { time = transaction.returnDate!!; clearTime() }

            val durationInDays = TimeUnit.MILLISECONDS.toDays(calReturn.timeInMillis - calRental.timeInMillis) + 1

            (durationInDays * transaction.rentalPricePerDay).coerceAtLeast(0.0)
        }
    }

    // Helper to clear time components from a Calendar instance for day-based calculations
    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // New daily profit
    val dailyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        // Start of today
        calendar.clearTime() // Use helper
        val todayStartMillis = calendar.timeInMillis

        // End of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val todayEndMillis = calendar.timeInMillis

        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(todayStartMillis, todayEndMillis)
            .map { transactions ->
                calculateProfitFromTransactions(transactions)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0.0
    )

    // New monthly profit
    val monthlyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        // Start of current month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.clearTime() // Use helper
        val monthStartMillis = calendar.timeInMillis

        // End of current month
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val monthEndMillis = calendar.timeInMillis

        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(monthStartMillis, monthEndMillis)
            .map { transactions ->
                calculateProfitFromTransactions(transactions)
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0.0
    )
}
