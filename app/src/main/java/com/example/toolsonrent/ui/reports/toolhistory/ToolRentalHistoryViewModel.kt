package com.example.toolsonrent.ui.reports.toolhistory

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer // Needed for Customer Name and customersMap
import com.example.toolsonrent.database.entity.RentalTransaction // Needed for transaction list type
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Date // Used by ToolRentalHistoryItem for date fields
// ToolRentalHistoryItem data class is in the same package.

@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest, a Flow operator
class ToolRentalHistoryViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // Used to retrieve toolId argument from navigation
) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val customerDao = AppDatabase.getInstance(application).customerDao() // To get customer names

    // Retrieves toolId from SavedStateHandle, passed via Navigation Component arguments.
    // This provides a Flow that emits the toolId or null if not present.
    private val toolIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("toolId", null)

    // StateFlow to hold the list of rental history items for the selected tool.
    // It uses flatMapLatest to switch to a new data flow if toolIdFlow emits a new ID.
    val rentalHistoryItems: StateFlow<List<ToolRentalHistoryItem>> = toolIdFlow.flatMapLatest { toolId ->
        if (toolId == null) {
            // If toolId is null, emit an empty list, indicating no data to load.
            flowOf(emptyList<ToolRentalHistoryItem>())
        } else {
            // If toolId is present, combine the tool's transactions with the list of all customers.
            // This allows mapping customerId to customerName efficiently.
            combine(
                transactionDao.getTransactionsForTool(toolId), // Flow<List<RentalTransaction>>
                customerDao.getAllCustomers()                  // Flow<List<Customer>>
            ) { transactions, customers ->
                // Create a map of customers for quick lookup by customer.id.
                val customersMap = customers.associateBy { it.id }
                val currentTime = System.currentTimeMillis()

                // Transform each transaction into a ToolRentalHistoryItem.
                transactions.mapNotNull { transaction ->
                    val customer = customersMap[transaction.customerId]
                    if (customer == null) {
                        // Log a warning if a customer referenced in a transaction is not found.
                        Log.w("ToolHistVM", "Customer data not found for transaction ${transaction.id}, customerId ${transaction.customerId}")
                        null // Exclude this item from the list if customer data is missing.
                    } else {
                        // Determine the status of the rental.
                        val status = when {
                            transaction.returnDate != null -> "Returned"
                            transaction.dueDate.time < currentTime -> "Overdue"
                            else -> "Active"
                        }
                        // Create the ToolRentalHistoryItem with all necessary details.
                        ToolRentalHistoryItem(
                            transactionId = transaction.id,
                            customerName = customer.name, // Use customer's name
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
