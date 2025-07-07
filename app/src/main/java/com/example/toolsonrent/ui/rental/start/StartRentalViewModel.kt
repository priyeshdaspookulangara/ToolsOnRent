package com.example.toolsonrent.ui.rental.start

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class StartRentalViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao() // Added
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao()

    val allCustomers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Renamed from availableTools to allToolTypes as it's for selecting the type first
    val allToolTypes: StateFlow<List<Tool>> = toolDao.getAllTools() // Fetches all tool types
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private val _selectedToolTypeId = MutableStateFlow<Int?>(null)

    // Exposes available instances for the currently selected tool type
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val availableToolInstances: StateFlow<List<ToolInstance>> = _selectedToolTypeId
        .flatMapLatest { typeId ->
            if (typeId == null) {
                flowOf(emptyList())
            } else {
                // Assuming "Available" is the status string for rentable instances
                toolInstanceDao.getInstancesForToolType(typeId)
                    .map { instances -> instances.filter { it.status == "Available" } }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSub Başkanlığı(5000L),
            initialValue = emptyList()
        )

    fun setSelectedToolType(toolTypeId: Int?) {
        _selectedToolTypeId.value = toolTypeId
    }


    private val _saveRentalResult = MutableLiveData<Result<Long>>() // Changed to Result<Long> for new transaction ID
    val saveRentalResult: LiveData<Result<Long>> = _saveRentalResult

    fun confirmRental(
        selectedCustomer: Customer?,
        selectedToolInstance: ToolInstance?, // Changed from selectedTool: Tool?
        rentalDate: Date?,
        dueDate: Date?,
        notes: String?,
        rentalPricePerDay: Double? // Price is now from the Tool Type, passed in
    ) {
        if (selectedCustomer == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a customer.")))
            return
        }
        if (selectedToolInstance == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a specific item to rent.")))
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
        if (rentalPricePerDay == null || rentalPricePerDay <= 0) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Invalid rental price.")))
            return
        }

        // Check instance status again, just in case
        if (selectedToolInstance.status != "Available") {
            Log.w("StartRentalViewModel", "Attempted to rent tool instance not in 'Available' state: ${selectedToolInstance.serialNumber ?: selectedToolInstance.instanceId} (Status: ${selectedToolInstance.status})")
            _saveRentalResult.postValue(Result.failure(IllegalStateException("Selected item (${selectedToolInstance.serialNumber ?: selectedToolInstance.instanceId}) is no longer available. Please select another item.")))
            return
        }

        val transaction = RentalTransaction(
            toolInstanceId = selectedToolInstance.instanceId, // Changed
            customerId = selectedCustomer.id,
            rentalDate = rentalDate,
            dueDate = dueDate,
            rentalPricePerDay = rentalPricePerDay, // Use passed-in price
            returnDate = null,
            notes = notes?.ifBlank { null }
        )

        val updatedInstance = selectedToolInstance.copy(status = "Rented") // New status

        viewModelScope.launch {
            // Ideally, these two operations (insert transaction, update instance) should be in a Room @Transaction
            try {
                val transactionId = rentalTransactionDao.insert(transaction)
                if (transactionId > 0) {
                    toolInstanceDao.update(updatedInstance)
                    _saveRentalResult.postValue(Result.success(transactionId))
                } else {
                    Log.e("StartRentalVM", "Failed to insert rental transaction for instance ${selectedToolInstance.instanceId}.")
                    _saveRentalResult.postValue(Result.failure(Exception("Failed to record rental transaction.")))
                }
            } catch (e: Exception) {
                Log.e("StartRentalViewModel", "Error confirming rental for instance ${selectedToolInstance.instanceId}", e)
                _saveRentalResult.postValue(Result.failure(e))
                // TODO: Consider manual rollback if only one part of a conceptual transaction fails.
                // For instance, if transaction insert succeeds but instance update fails.
                // This is where Room @Transaction methods are beneficial.
            }
        }
    }
}
