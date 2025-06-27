package com.example.toolsonrent.ui.rental.start

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date

class StartRentalViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao()

    val allCustomers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // availableTools now correctly filters by currentAvailableQuantity > 0 in ToolDao
    val availableTools: StateFlow<List<Tool>> = toolDao.getAvailableTools()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _saveRentalResult = MutableLiveData<Result<Unit>>()
    val saveRentalResult: LiveData<Result<Unit>> = _saveRentalResult

    fun confirmRental(
        selectedCustomer: Customer?,
        selectedTool: Tool?,
        rentalDate: Date?,
        dueDate: Date?,
        notes: String?
    ) {
        if (selectedCustomer == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a customer.")))
            return
        }
        if (selectedTool == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a tool.")))
            return
        }
        if (rentalDate == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a rental date.")))
            return
        }
        if (dueDate == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a due date.")))
            return
        }
        if (dueDate.before(rentalDate)) {
             _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Due date cannot be before rental date.")))
            return
        }

        // Updated availability check based on quantity
        if (selectedTool.currentAvailableQuantity <= 0) {
            Log.w("StartRentalViewModel", "Attempted to rent tool with zero available quantity: ${selectedTool.name} (ID: ${selectedTool.id})")
            _saveRentalResult.postValue(Result.failure(IllegalStateException("Selected tool (${selectedTool.name}) has no available quantity. Please select another tool.")))
            return
        }

        val transaction = RentalTransaction(
            toolId = selectedTool.id,
            customerId = selectedCustomer.id,
            rentalDate = rentalDate,
            dueDate = dueDate,
            rentalPricePerDay = selectedTool.rentalPrice,
            returnDate = null,
            notes = notes?.ifBlank { null }
        )

        viewModelScope.launch {
            try {
                // Insert the rental transaction first
                val transactionId = rentalTransactionDao.insert(transaction)

                if (transactionId > 0) { // Check if insert was successful (rowId > 0)
                    // Then, attempt to decrement the tool's available quantity
                    val rowsUpdated = toolDao.decrementAvailableQuantity(selectedTool.id, 1)
                    if (rowsUpdated > 0) { // Check if decrement was successful (at least one row updated)
                        _saveRentalResult.postValue(Result.success(Unit))
                    } else {
                        // Decrement failed (e.g., quantity became 0 concurrently by another operation)
                        // This indicates a potential data inconsistency or race condition.
                        // Ideal: Rollback the inserted transaction.
                        Log.e("StartRentalVM", "Inserted transaction $transactionId but failed to decrement quantity for tool ${selectedTool.id}. Manual rollback might be needed or use @Transaction.")
                        _saveRentalResult.postValue(Result.failure(
                            IllegalStateException("Tool quantity became unavailable during the transaction process. Please try again or select a different tool.")
                        ))
                        // TODO: Implement actual rollback for transactionId if decrement fails after insert.
                        // This would typically involve a @Transaction annotated method in a DAO that calls both insert and decrement.
                    }
                } else {
                    Log.e("StartRentalVM", "Failed to insert rental transaction for tool ${selectedTool.id}.")
                    _saveRentalResult.postValue(Result.failure(Exception("Failed to record rental transaction.")))
                }
            } catch (e: Exception) {
                Log.e("StartRentalViewModel", "Error confirming rental for tool ${selectedTool.id}", e)
                _saveRentalResult.postValue(Result.failure(e))
            }
        }
    }
}
