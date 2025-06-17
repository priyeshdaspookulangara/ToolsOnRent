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

    val availableTools: StateFlow<List<Tool>> = toolDao.getAvailableTools() // This DAO method was added previously
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
        // This check is against the selectedTool object passed from fragment.
        // The fragment should ideally be observing 'availableTools' which are already filtered.
        // This serves as a final validation or safeguard.
        if (!selectedTool.isAvailable) {
            Log.w("StartRentalViewModel", "Attempted to rent an already unavailable tool: ${selectedTool.name} (ID: ${selectedTool.id})")
            _saveRentalResult.postValue(Result.failure(IllegalStateException("Selected tool (${selectedTool.name}) is no longer available. Please select another tool.")))
            return
        }

        val transaction = RentalTransaction(
            toolId = selectedTool.id,
            customerId = selectedCustomer.id,
            rentalDate = rentalDate,
            dueDate = dueDate,
            rentalPricePerDay = selectedTool.rentalPrice, // Price captured at the time of rental
            returnDate = null, // Not returned yet
            notes = notes?.ifBlank { null } // Store null if notes are blank
        )

        viewModelScope.launch {
            try {
                // For true atomicity, these operations should be wrapped in a database transaction.
                // This can be done by creating a @Transaction annotated method in a DAO
                // or by using AppDatabase.withTransaction { ... } directly here.
                // The current sequential execution is simpler for this step but not fully atomic.
                rentalTransactionDao.insert(transaction)

                // Update the tool's availability status
                val toolToUpdate = selectedTool.copy(isAvailable = false)
                toolDao.update(toolToUpdate)

                _saveRentalResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("StartRentalViewModel", "Error confirming rental", e)
                _saveRentalResult.postValue(Result.failure(e))
                // Note: If transaction insert succeeded but tool update failed,
                // the system would be in an inconsistent state without a DB transaction rollback.
            }
        }
    }
}
