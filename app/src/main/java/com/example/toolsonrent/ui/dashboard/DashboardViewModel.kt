package com.example.toolsonrent.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.RentalTransaction // For helper function and activeRentalsFlow
import com.prolificinteractive.materialcalendarview.CalendarDay // For calendar decoration
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date // For Date.toCalendarDay() extension
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao()

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
            if (transaction.returnDate == null) return@sumOf 0.0

            val calRental = Calendar.getInstance().apply { time = transaction.rentalDate; clearTime() }
            val calReturn = Calendar.getInstance().apply { time = transaction.returnDate!!; clearTime() }

            val durationInDays = TimeUnit.MILLISECONDS.toDays(calReturn.timeInMillis - calRental.timeInMillis) + 1

            (durationInDays * transaction.rentalPricePerDay).coerceAtLeast(0.0)
        }
    }

    private fun Calendar.clearTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val dailyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.clearTime()
        val todayStartMillis = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val todayEndMillis = calendar.timeInMillis

        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(todayStartMillis, todayEndMillis)
            .map { transactions -> calculateProfitFromTransactions(transactions) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val monthlyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.clearTime()
        val monthStartMillis = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val monthEndMillis = calendar.timeInMillis

        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(monthStartMillis, monthEndMillis)
            .map { transactions -> calculateProfitFromTransactions(transactions) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // --- Calendar Decoration Logic ---

    // Helper function to convert java.util.Date to MaterialCalendarView's CalendarDay
    private fun Date.toCalendarDay(): CalendarDay {
        val cal = Calendar.getInstance()
        cal.time = this
        return CalendarDay.from( // CalendarDay month is 1-12, Calendar month is 0-11
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Helper function to get start of today in milliseconds
    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.clearTime() // Uses the extension function defined above
        return calendar.timeInMillis
    }

    // Base flow of active rentals (not yet returned)
    private val activeRentalsFlow: Flow<List<RentalTransaction>> =
        rentalTransactionDao.getActiveRentals()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), replay = 1) // Share to avoid multiple DAO calls

    // StateFlow for Due Today CalendarDays
    val dueTodayCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val todayStartMillis = getStartOfTodayMillis()
        // End of today is start of today + 1 day - 1 millisecond
        val todayEndMillis = todayStartMillis + TimeUnit.DAYS.toMillis(1) - 1

        transactions.filter {
            // Check if dueDate falls within the milliseconds range of today
            it.dueDate.time in todayStartMillis..todayEndMillis
        }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    // StateFlow for Overdue (and not yet returned) CalendarDays
    val overdueUnreturnedCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val todayStartMillis = getStartOfTodayMillis()
        transactions.filter {
            it.dueDate.time < todayStartMillis // Due date is before start of today
            // No need to check for returnDate == null as activeRentalsFlow already filters for this
        }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    // StateFlow for Upcoming (due from tomorrow onwards) Return CalendarDays
    val upcomingReturnCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val todayStartMillis = getStartOfTodayMillis()
        val tomorrowStartMillis = todayStartMillis + TimeUnit.DAYS.toMillis(1) // Start of tomorrow

        transactions.filter {
            it.dueDate.time >= tomorrowStartMillis // Due date is from tomorrow onwards
        }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())
}
