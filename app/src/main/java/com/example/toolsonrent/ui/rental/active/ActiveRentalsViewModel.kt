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
    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val customerDao = AppDatabase.getInstance(application).customerDao()

    val activeRentalItems: StateFlow<List<ActiveRentalInfo>> =
        combine(
            transactionDao.getActiveRentals(),
            toolDao.getAllTools(),
            customerDao.getAllCustomers()
        ) { activeTransactions, tools, customers ->
            val toolsMap = tools.associateBy { it.id }
            val customersMap = customers.associateBy { it.id }

            activeTransactions.mapNotNull { transaction ->
                val tool = toolsMap[transaction.toolId]
                val customer = customersMap[transaction.customerId]

                if (tool != null && customer != null) {
                    ActiveRentalInfo(
                        transactionId = transaction.id,
                        toolId = transaction.toolId,
                        toolName = tool.name,
                        customerName = customer.name,
                        rentalDate = transaction.rentalDate,
                        dueDate = transaction.dueDate
                    )
                } else {
                    Log.w(
                        "ActiveRentalsVM",
                        "Data inconsistency: Missing tool (ID: ${transaction.toolId}) or " +
                        "customer (ID: ${transaction.customerId}) for active transaction ID: ${transaction.id}"
                    )
                    null
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _rentalCompletionResult = MutableLiveData<Result<Unit>>()
    val rentalCompletionResult: LiveData<Result<Unit>> = _rentalCompletionResult

    fun completeRental(transactionId: Int, toolId: Int, returnDate: Date) {
        viewModelScope.launch {
            try {
                // Note: These operations should ideally be in a single database transaction
                // for atomicity (e.g., using Room's @Transaction on a DAO method or
                // AppDatabase.withTransaction { ... }).

                // 1. Fetch the transaction
                val transaction = transactionDao.getTransactionById(transactionId).firstOrNull()
                                  ?: throw IllegalStateException("Transaction with ID $transactionId not found.")

                // 2. Check if already returned
                if (transaction.returnDate != null) {
                    // Post failure if already completed, but perhaps log it differently or handle as idempotent success
                    _rentalCompletionResult.postValue(Result.failure(IllegalStateException("Rental for transaction ID $transactionId has already been completed.")))
                    return@launch
                }

                // 3. Update the transaction with the return date
                val updatedTransaction = transaction.copy(returnDate = returnDate)
                rentalTransactionDao.update(updatedTransaction)

                // 4. Fetch the associated tool
                // Ensure toolDao.getToolById returns Flow<Tool?>
                val tool = toolDao.getToolById(toolId).firstOrNull()
                           ?: throw IllegalStateException("Tool with ID $toolId not found for this transaction.")

                // 5. Update the tool's availability
                val updatedTool = tool.copy(isAvailable = true)
                toolDao.update(updatedTool)

                _rentalCompletionResult.postValue(Result.success(Unit))

            } catch (e: Exception) {
                Log.e("ActiveRentalsVM", "Error completing rental for transaction ID $transactionId, Tool ID $toolId", e)
                _rentalCompletionResult.postValue(Result.failure(e))
            }
        }
    }
}
