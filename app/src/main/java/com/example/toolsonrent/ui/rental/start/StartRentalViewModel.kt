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
    private val toolDao = AppDatabase.getInstance(application).toolDao() // For Tool (type) info
    private val toolItemDao = AppDatabase.getInstance(application).toolItemDao() // For ToolItem info
    private val rentalTransactionDao = AppDatabase.getInstance(application).rentalTransactionDao()

    val allCustomers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Renamed: User first selects a tool type (template)
    val allToolTypes: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Holds the ID of the currently selected tool type by the user in the UI
    val selectedToolTypeId = MutableStateFlow<Int?>(null)

    // Holds the list of available items for the selected tool type
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val availableToolItems: StateFlow<List<com.example.toolsonrent.database.entity.ToolItem>> = selectedToolTypeId.flatMapLatest { typeId ->
        if (typeId != null && typeId != 0) {
            toolItemDao.getAvailableItemsByToolType(typeId)
        } else {
            flowOf(emptyList()) // No type selected or invalid ID, so no items
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    private val _saveRentalResult = MutableLiveData<Result<Long>>() // Return transaction ID
    val saveRentalResult: LiveData<Result<Long>> = _saveRentalResult

    // Status constant - consider moving to a shared constants file or enum
    private val STATUS_ON_RENT = "On Rent"
    private val STATUS_AVAILABLE = "Available"


    fun confirmRental(
        selectedCustomer: Customer?,
        selectedToolItem: com.example.toolsonrent.database.entity.ToolItem?, // Changed from Tool to ToolItem
        rentalDate: Date?,
        dueDate: Date?,
        notes: String?
    ) {
        if (selectedCustomer == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a customer.")))
            return
        }
        if (selectedToolItem == null) {
            _saveRentalResult.postValue(Result.failure(IllegalArgumentException("Please select a specific tool item.")))
            return
        }
        if (selectedToolItem.status != STATUS_AVAILABLE) {
             _saveRentalResult.postValue(Result.failure(IllegalStateException("Selected item '${selectedToolItem.unitIdUser}' is not available.")))
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

        viewModelScope.launch {
            try {
                // Fetch the tool type to get the default rental price
                val toolType = toolDao.getToolById(selectedToolItem.toolTypeId).firstOrNull()
                if (toolType == null) {
                    _saveRentalResult.postValue(Result.failure(IllegalStateException("Could not find tool type details for the selected item.")))
                    return@launch
                }

                val transaction = RentalTransaction(
                    toolItemId = selectedToolItem.id, // Use ToolItem's ID
                    customerId = selectedCustomer.id,
                    rentalDate = rentalDate,
                    dueDate = dueDate,
                    rentalPricePerDay = toolType.rentalPrice, // Price from Tool Type
                    returnDate = null,
                    notes = notes?.ifBlank { null }
                )

                // Ideally, this block (insert transaction + update item status) should be in a single DB transaction.
                // This can be achieved by creating a @Transaction annotated method in a DAO that calls both operations.
                // For simplicity here, we do them sequentially.

                val transactionId = rentalTransactionDao.insert(transaction)
                if (transactionId > 0) {
                    toolItemDao.updateItemStatus(selectedToolItem.id, STATUS_ON_RENT)
                    _saveRentalResult.postValue(Result.success(transactionId))
                } else {
                    Log.e("StartRentalVM", "Failed to insert rental transaction for item ${selectedToolItem.id}.")
                    _saveRentalResult.postValue(Result.failure(Exception("Failed to record rental transaction.")))
                }
            } catch (e: Exception) {
                Log.e("StartRentalViewModel", "Error confirming rental for item ${selectedToolItem?.id}", e)
                _saveRentalResult.postValue(Result.failure(e))
            }
        }
    }
}
