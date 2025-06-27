package com.example.toolsonrent.ui.dashboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.ui.dashboard.calendar.details.DailyRentalDetailItem
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val customerDao = AppDatabase.getInstance(application).customerDao()

    // Base list of all tools for deriving quantity-based counts
    private val allToolsListDashboard: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Updated availableToolsCount: sum of currentAvailableQuantity for all tools
    val availableToolsCount: StateFlow<Int> = allToolsListDashboard.map { tools ->
        tools.sumOf { it.currentAvailableQuantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // Updated rentedToolsCount: sum of (totalQuantity - currentAvailableQuantity) for all tools
    val rentedToolsCount: StateFlow<Int> = allToolsListDashboard.map { tools ->
        tools.sumOf { it.totalQuantity - it.currentAvailableQuantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    val overdueToolsCount: StateFlow<Int> = rentalTransactionDao.getOverdueRentals(System.currentTimeMillis())
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

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
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    val dailyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.clearTime()
        val todayStartMillis = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val todayEndMillis = calendar.timeInMillis
        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(todayStartMillis, todayEndMillis)
            .map { calculateProfitFromTransactions(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val monthlyProfit: StateFlow<Double> = flow {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.clearTime()
        val monthStartMillis = calendar.timeInMillis
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val monthEndMillis = calendar.timeInMillis
        emitAll(rentalTransactionDao.getCompletedTransactionsInRange(monthStartMillis, monthEndMillis)
            .map { calculateProfitFromTransactions(it) })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    private fun Date.toCalendarDay(): CalendarDay {
        val cal = Calendar.getInstance(); cal.time = this
        return CalendarDay.from(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    private fun getStartOfTodayMillis(): Long {
        val calendar = Calendar.getInstance(); calendar.clearTime(); return calendar.timeInMillis
    }

    private val activeRentalsFlow: Flow<List<RentalTransaction>> =
        rentalTransactionDao.getActiveRentals().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), replay = 1)

    val dueTodayCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val todayStartMillis = getStartOfTodayMillis()
        val todayEndMillis = todayStartMillis + TimeUnit.DAYS.toMillis(1) - 1
        transactions.filter { it.dueDate.time in todayStartMillis..todayEndMillis }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val overdueUnreturnedCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val todayStartMillis = getStartOfTodayMillis()
        transactions.filter { it.dueDate.time < todayStartMillis }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val upcomingReturnCalendarDays: StateFlow<Set<CalendarDay>> = activeRentalsFlow.map { transactions ->
        val tomorrowStartMillis = getStartOfTodayMillis() + TimeUnit.DAYS.toMillis(1)
        transactions.filter { it.dueDate.time >= tomorrowStartMillis }.map { it.dueDate.toCalendarDay() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    private val _selectedCalendarDayForDetails = MutableStateFlow<CalendarDay?>(null)
    val selectedCalendarDayForDetails: StateFlow<CalendarDay?> = _selectedCalendarDayForDetails.asStateFlow()

    fun userSelectedDateForDetails(calendarDay: CalendarDay?) {
        _selectedCalendarDayForDetails.value = calendarDay
        Log.d("DashboardVM", "User selected date for details: $calendarDay")
    }

    val rentalsForSelectedDate: StateFlow<List<DailyRentalDetailItem>> =
        _selectedCalendarDayForDetails.flatMapLatest { selectedDay ->
            if (selectedDay == null) {
                flowOf(emptyList())
            } else {
                combine(
                    activeRentalsFlow,
                    toolDao.getAllTools(),
                    customerDao.getAllCustomers()
                ) { transactions, tools, customers ->
                    val toolsMap = tools.associateBy { it.id }
                    val customersMap = customers.associateBy { it.id }
                    val currentCalDay = CalendarDay.today()

                    transactions.filter { transaction ->
                        transaction.dueDate.toCalendarDay() == selectedDay
                    }.mapNotNull { transaction ->
                        val tool = toolsMap[transaction.toolId]
                        val customer = customersMap[transaction.customerId]

                        if (tool != null && customer != null) {
                            val dueCalDay = selectedDay
                            val status = when {
                                dueCalDay.isBefore(currentCalDay) -> "Overdue (Was due this day)"
                                dueCalDay == currentCalDay -> "Due Today"
                                else -> "Upcoming (Due this day)"
                            }
                            DailyRentalDetailItem(
                                toolName = tool.name,
                                customerName = customer.name,
                                fullDueDate = transaction.dueDate,
                                status = status,
                                transactionId = transaction.id
                            )
                        } else {
                            Log.w("DashboardVM", "Tool or Customer not found for transaction ${transaction.id} while generating daily details.")
                            null
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
}
