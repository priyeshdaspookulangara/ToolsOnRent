package com.example.toolsonrent.ui.reports.customerhistory

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool // Needed for Tool Name and toolsMap
import com.example.toolsonrent.database.entity.RentalTransaction // Needed for transaction list type
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Date // Used in CustomerRentalHistoryItem
// CustomerRentalHistoryItem data class is in the same package.

@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest
class CustomerRentalHistoryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // Used to retrieve customerId argument
) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao()

    // Retrieves customerId from SavedStateHandle, passed via Navigation Component arguments.
    // This provides a Flow that emits the customerId or null if not present.
    private val customerIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("customerId", null)

    // StateFlow to hold the list of rental history items for the selected customer.
    // It uses flatMapLatest to switch to a new data flow if customerIdFlow emits a new ID.
    val rentalHistoryItems: StateFlow<List<CustomerRentalHistoryItem>> = customerIdFlow.flatMapLatest { customerId ->
        if (customerId == null) {
            // If customerId is null, emit an empty list, indicating no data to load.
            flowOf(emptyList<CustomerRentalHistoryItem>())
        } else {
            // If customerId is present, combine the customer's transactions with the list of all tools.
            // This allows mapping toolId to toolName efficiently.
            combine(
                transactionDao.getTransactionsForCustomer(customerId), // Flow<List<RentalTransaction>>
                toolDao.getAllTools()                                 // Flow<List<Tool>>
            ) { transactions, tools ->
                // Create a map of tools for quick lookup by tool.id.
                val toolsMap = tools.associateBy { it.id }
                val currentTime = System.currentTimeMillis()

                // Transform each transaction into a CustomerRentalHistoryItem.
                transactions.mapNotNull { transaction ->
                    val tool = toolsMap[transaction.toolId]
                    if (tool == null) {
                        // Log a warning if a tool referenced in a transaction is not found.
                        Log.w("CustHistVM", "Tool data not found for transaction ${transaction.id}, toolId ${transaction.toolId}")
                        null // Exclude this item from the list if tool data is missing.
                    } else {
                        // Determine the status of the rental.
                        val status = when {
                            transaction.returnDate != null -> "Returned"
                            transaction.dueDate.time < currentTime -> "Overdue"
                            else -> "Active"
                        }
                        // Create the CustomerRentalHistoryItem with all necessary details.
                        CustomerRentalHistoryItem(
                            transactionId = transaction.id,
                            toolName = tool.name,
                            rentalDate = transaction.rentalDate,
                            dueDate = transaction.dueDate,
                            returnDate = transaction.returnDate,
                            rentalPricePerDay = transaction.rentalPricePerDay,
                            status = status
                        )
                    }
                }
            }
        }
    }.stateIn(
        scope = viewModelScope, // Managed within the ViewModel's lifecycle.
        started = SharingStarted.WhileSubscribed(5000L), // Keeps flow active 5s after last collector stops.
        initialValue = emptyList() // Initial value before any data is loaded.
    )
}
