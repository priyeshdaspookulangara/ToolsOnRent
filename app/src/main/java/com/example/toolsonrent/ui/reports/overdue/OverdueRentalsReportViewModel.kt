package com.example.toolsonrent.ui.reports.overdue

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
// Tool and Customer entities are implicitly used by DAOs, direct import not strictly needed here.
// import com.example.toolsonrent.database.entity.Customer
// import com.example.toolsonrent.database.entity.Tool
// RentalTransaction is used by transactionDao.getOverdueRentals
import com.example.toolsonrent.database.entity.RentalTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Date // Used by OverdueRentalInfo
import java.util.concurrent.TimeUnit

// OverdueRentalInfo data class from the same package is implicitly available.

@OptIn(ExperimentalCoroutinesApi::class) // For potential future use of other experimental Flow operators. `combine` is stable.
class OverdueRentalsReportViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val customerDao = AppDatabase.getInstance(application).customerDao()

    // This StateFlow will hold the list of OverdueRentalInfo objects.
    // It combines data from three sources: overdue transactions, all tools, and all customers.
    val overdueRentalItems: StateFlow<List<OverdueRentalInfo>> =
        combine(
            // Flow 1: Get overdue rental transactions. System.currentTimeMillis() provides the current time for the query.
            transactionDao.getOverdueRentals(System.currentTimeMillis()), // Flow<List<RentalTransaction>>
            // Flow 2: Get all tools. This is needed to map toolId to toolName.
            toolDao.getAllTools(), // Flow<List<Tool>>
            // Flow 3: Get all customers. This is needed to map customerId to customerName.
            customerDao.getAllCustomers() // Flow<List<Customer>>
        ) { overdueTransactions, tools, customers ->
            // This lambda block is the 'transform' function of the combine operator.
            // It's executed whenever any of the three upstream flows emit a new value.

            // Create maps for quick lookup of tool and customer names by their IDs.
            val toolsMap = tools.associateBy { it.id }
            val customersMap = customers.associateBy { it.id }

            // Map each RentalTransaction to an OverdueRentalInfo object.
            overdueTransactions.mapNotNull { transaction ->
                val tool = toolsMap[transaction.toolId]
                val customer = customersMap[transaction.customerId]

                // Only proceed if both tool and customer data are found for the transaction.
                // This handles potential data inconsistencies gracefully.
                if (tool != null && customer != null) {
                    // Calculate how many days the rental is overdue.
                    // transaction.dueDate.time gives the due date in milliseconds.
                    // System.currentTimeMillis() gives the current time in milliseconds.
                    val overdueMillis = System.currentTimeMillis() - transaction.dueDate.time
                    val daysOverdue = TimeUnit.MILLISECONDS.toDays(overdueMillis)
                        .coerceAtLeast(0L) // Ensures daysOverdue is not negative (e.g., if due date is slightly in future due to timing).

                    OverdueRentalInfo(
                        transactionId = transaction.id,
                        toolName = tool.name,
                        customerName = customer.name,
                        dueDate = transaction.dueDate, // The original due date from the transaction.
                        daysOverdue = daysOverdue
                    )
                } else {
                    // If tool or customer info is missing, log a warning and skip this transaction.
                    Log.w(
                        "OverdueVM",
                        "Missing tool (ID: ${transaction.toolId}) or customer (ID: ${transaction.customerId}) for transaction ID: ${transaction.id}"
                    )
                    null // mapNotNull will filter out null values.
                }
            }
        }.stateIn(
            scope = viewModelScope, // The scope in which the StateFlow is managed.
            started = SharingStarted.WhileSubscribed(5000L), // The upstream flows start when there's a subscriber and stop 5s after the last one leaves.
            initialValue = emptyList() // Initial empty list before data is loaded.
        )
}
