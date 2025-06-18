package com.example.toolsonrent.ui.customer.edit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest usage
class EditCustomerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // Injected by ViewModelProvider
) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()

    // Flow for the customerId obtained from navigation arguments via SavedStateHandle.
    // Defaulting to null if not found, though it should always be passed for this screen.
    private val customerIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("customerId", null)

    // StateFlow to hold the Customer object being edited.
    // It reacts to changes in customerIdFlow (though customerId typically won't change for an instance of this ViewModel).
    // It fetches the customer details from the DAO.
    val customer: StateFlow<Customer?> = customerIdFlow.flatMapLatest { id ->
        if (id != null && id != 0) { // Room auto-generated IDs start from 1. 0 is not a valid ID.
            customerDao.getCustomerById(id) // This DAO method returns Flow<Customer?>
        } else {
            Log.w("EditCustomerVM", "Invalid customerId ($id) received.")
            flowOf(null) // Emit null if customerId is invalid or not present.
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null // Initially null until customerId is processed and data is fetched.
    )

    // LiveData to communicate the result of the update operation back to the Fragment.
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    fun updateCustomer(
        currentCustomerId: Int, // Explicitly pass the ID to ensure we're updating the correct entity.
        name: String,
        phone: String,
        email: String?,
        address: String?
    ) {
        // Basic validation for required fields.
        if (name.isBlank() || phone.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Name and Phone cannot be empty.")))
            return
        }
        // Ensure we have a valid ID (should match the one loaded).
        if (currentCustomerId == 0) {
             _updateResult.postValue(Result.failure(IllegalStateException("Invalid Customer ID provided for update.")))
            return
        }

        // Create a new Customer object with the updated details and the original ID.
        val updatedCustomer = Customer(
            id = currentCustomerId,
            name = name,
            phoneNumber = phone,
            email = email?.ifBlank { null }, // Store null if email is blank.
            address = address?.ifBlank { null } // Store null if address is blank.
        )

        viewModelScope.launch {
            try {
                customerDao.update(updatedCustomer) // Perform the update operation.
                _updateResult.postValue(Result.success(Unit)) // Notify success.
            } catch (e: Exception) {
                Log.e("EditCustomerVM", "Error updating customer ID $currentCustomerId", e)
                _updateResult.postValue(Result.failure(e)) // Notify failure.
            }
        }
    }


    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun deleteCustomer() {
        val customerToDelete = customer.value // Get the current customer loaded by the StateFlow

        if (customerToDelete == null) {
            _deleteResult.postValue(Result.failure(IllegalStateException("No customer loaded or customer ID is invalid, cannot delete.")))
            return
        }
        // Double check ID just in case, though customer.value should be authoritative if loaded
        if (customerToDelete.id == 0) {
             _deleteResult.postValue(Result.failure(IllegalStateException("Cannot delete customer with invalid ID.")))
            return
        }

        viewModelScope.launch {
            try {
                customerDao.delete(customerToDelete)
                _deleteResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                // Specific error handling for foreign key constraint violation (SQLiteConstraintException)
                // can be added here if we want to provide a more specific message to the user.
                // For example, by checking if e is an instance of SQLiteConstraintException.
                Log.e("EditCustomerVM", "Error deleting customer ${customerToDelete.id}", e)
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}
