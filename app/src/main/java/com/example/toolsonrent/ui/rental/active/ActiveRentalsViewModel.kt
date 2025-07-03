package com.example.toolsonrent.ui.rental.active

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool // Needed for tool.copy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull // For getting single value from Flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch // For launching the coroutine in completeRental
import java.util.Date // For returnDate parameter and transaction.copy

class ActiveRentalsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao() // To get tool type name
    private val toolItemDao = AppDatabase.getInstance(application).toolItemDao() // To update item status
    private val customerDao = AppDatabase.getInstance(application).customerDao()

    // Define status constants (could be in a shared file/enum)
    object ToolItemStatus {
        const val AVAILABLE = "Available"
        const val ON_RENT = "On Rent"
        const val MAINTENANCE = "Maintenance"
        const val DAMAGED = "Damaged"
        // Add other statuses from your list: Missing, Retired/Scrapped, Reserved
    }

    val activeRentalItems: StateFlow<List<ActiveRentalInfo>> =
        combine(
            transactionDao.getActiveRentals(), // Flow<List<RentalTransaction>>
            customerDao.getAllCustomers(),     // Flow<List<Customer>>
            toolItemDao.getAllToolItems(),     // Changed from getItemsByToolType(-1)
            toolDao.getAllTools()              // To map toolTypeId to toolTypeName
        ) { activeTransactions, customers, allToolItems, allToolTypes ->
            val customersMap = customers.associateBy { it.id }
            val toolItemsMap = allToolItems.associateBy { it.id }
            val toolTypesMap = allToolTypes.associateBy { it.id }

            activeTransactions.mapNotNull { transaction ->
                val customer = customersMap[transaction.customerId]
                val toolItem = toolItemsMap[transaction.toolItemId] // transaction now has toolItemId

                if (toolItem == null) {
                     Log.w("ActiveRentalsVM", "Data inconsistency: Missing tool item (ID: ${transaction.toolItemId}) for transaction ID: ${transaction.id}")
                     return@mapNotNull null
                }
                val toolType = toolTypesMap[toolItem.toolTypeId]

                if (customer != null && toolType != null) {
                    ActiveRentalInfo(
                        transactionId = transaction.id,
                        toolItemId = transaction.toolItemId,
                        unitIdUser = toolItem.unitIdUser,
                        toolTypeName = toolType.name,
                        customerName = customer.name,
                        rentalDate = transaction.rentalDate,
                        dueDate = transaction.dueDate
                    )
                } else {
                    Log.w(
                        "ActiveRentalsVM",
                        "Data inconsistency: Missing customer (ID: ${transaction.customerId}) or " +
                        "tool type (ID: ${toolItem.toolTypeId}) for item ID ${toolItem.id}, transaction ID: ${transaction.id}"
                    )
                    null
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Consider a longer timeout if DB operations are slow
            initialValue = emptyList()
        )

    private val _rentalCompletionResult = MutableLiveData<Result<Unit>>()
    val rentalCompletionResult: LiveData<Result<Unit>> = _rentalCompletionResult

    fun returnTool(
        transactionId: Int,
        toolItemId: Int,
        returnDate: Date,
        newItemStatus: String = ToolItemStatus.AVAILABLE, // Default to Available on return
        newItemCondition: String? = null // Optional: update condition on return
    ) {
        viewModelScope.launch {
            try {
                // These operations should ideally be in a single database transaction.
                val transaction = transactionDao.getTransactionById(transactionId).firstOrNull()
                    ?: throw IllegalStateException("Transaction with ID $transactionId not found.")

                if (transaction.returnDate != null) {
                    _rentalCompletionResult.postValue(Result.failure(IllegalStateException("Rental $transactionId already completed.")))
                    return@launch
                }

                val updatedTransaction = transaction.copy(returnDate = returnDate)
                transactionDao.update(updatedTransaction)

                toolItemDao.updateItemStatus(toolItemId, newItemStatus)

                if (newItemCondition != null) {
                    val toolItem = toolItemDao.getItemById(toolItemId).firstOrNull()
                    if (toolItem != null) {
                        toolItemDao.update(toolItem.copy(condition = newItemCondition))
                    } else {
                        Log.w("ActiveRentalsVM", "Could not find ToolItem $toolItemId to update condition.")
                    }
                }
                _rentalCompletionResult.postValue(Result.success(Unit))

            } catch (e: Exception) {
                Log.e("ActiveRentalsVM", "Error completing rental for transaction $transactionId, Item ID $toolItemId", e)
                _rentalCompletionResult.postValue(Result.failure(e))
            }
        }
    }
}
