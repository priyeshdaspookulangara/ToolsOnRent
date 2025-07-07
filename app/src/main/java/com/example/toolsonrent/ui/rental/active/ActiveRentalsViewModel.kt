package com.example.toolsonrent.ui.rental.active

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class ActiveRentalsViewModel(application: Application) : AndroidViewModel(application) {

    private val transactionDao = AppDatabase.getInstance(application).rentalTransactionDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val customerDao = AppDatabase.getInstance(application).customerDao()
    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao() // Added

    val activeRentalItems: StateFlow<List<ActiveRentalInfo>> =
        transactionDao.getActiveRentals()
            .flatMapLatest { activeTransactions ->
                if (activeTransactions.isEmpty()) {
                    return@flatMapLatest flowOf(emptyList<ActiveRentalInfo>())
                }
                // Fetch all customers, tools (types), and instances once
                val customersFlow = customerDao.getAllCustomers()
                val toolTypesFlow = toolDao.getAllTools()
                // We need all instances to map them, or fetch them individually if performance is an issue
                // For simplicity, let's fetch all. If too many instances, this might need optimization.
                val toolInstancesFlow = toolInstanceDao.getAllInstances()

                combine(customersFlow, toolTypesFlow, toolInstancesFlow) { customers, toolTypes, toolInstances ->
                    val customersMap = customers.associateBy { it.id }
                    val toolInstancesMap = toolInstances.associateBy { it.instanceId }
                    val toolTypesMap = toolTypes.associateBy { it.id }

                    activeTransactions.mapNotNull { transaction ->
                        val customer = customersMap[transaction.customerId]
                        val instance = toolInstancesMap[transaction.toolInstanceId]
                        val toolType = instance?.let { toolTypesMap[it.toolTypeId] }

                        if (customer != null && instance != null && toolType != null) {
                            ActiveRentalInfo(
                                transactionId = transaction.id,
                                toolInstanceId = instance.instanceId,
                                toolTypeName = toolType.name,
                                toolInstanceIdentifier = if (!instance.serialNumber.isNullOrBlank()) "SN: ${instance.serialNumber}" else "ID: ${instance.instanceId}",
                                customerName = customer.name,
                                rentalDate = transaction.rentalDate,
                                dueDate = transaction.dueDate,
                                toolImageUri = toolType.imageUri // Image of the tool type
                            )
                        } else {
                            Log.w("ActiveRentalsVM", "Data inconsistency for active transaction ID: ${transaction.id}. Missing customer, instance, or tool type.")
                            null
                        }
                    }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    private val _rentalCompletionResult = MutableLiveData<Result<Unit>>()
    val rentalCompletionResult: LiveData<Result<Unit>> = _rentalCompletionResult

    // Changed toolId to toolInstanceId
    fun completeRental(transactionId: Int, toolInstanceId: Int, returnDate: Date, newStatus: String = "Available") {
        viewModelScope.launch {
            // These operations should ideally be in a single Room @Transaction
            try {
                val transaction = transactionDao.getTransactionById(transactionId).firstOrNull()
                    ?: throw IllegalStateException("Transaction with ID $transactionId not found.")

                if (transaction.returnDate != null) {
                    _rentalCompletionResult.postValue(Result.failure(IllegalStateException("Rental for transaction ID $transactionId has already been completed.")))
                    return@launch
                }

                // Ensure the transaction's instanceId matches the provided one, just as a sanity check
                if (transaction.toolInstanceId != toolInstanceId) {
                    throw IllegalStateException("Mismatch: Transaction $transactionId is for instance ${transaction.toolInstanceId}, but return was requested for $toolInstanceId.")
                }

                val toolInstance = toolInstanceDao.getInstanceById(toolInstanceId).firstOrNull()
                    ?: throw IllegalStateException("ToolInstance with ID $toolInstanceId not found for return.")

                // Update the transaction with the return date
                val updatedTransaction = transaction.copy(returnDate = returnDate)
                transactionDao.update(updatedTransaction)

                // Update the ToolInstance status
                val updatedInstance = toolInstance.copy(status = newStatus) // Default to "Available"
                toolInstanceDao.update(updatedInstance)

                _rentalCompletionResult.postValue(Result.success(Unit))

            } catch (e: Exception) {
                Log.e("ActiveRentalsVM", "Error completing rental for transaction ID $transactionId, ToolInstance ID $toolInstanceId", e)
                _rentalCompletionResult.postValue(Result.failure(e))
            }
        }
    }
}
