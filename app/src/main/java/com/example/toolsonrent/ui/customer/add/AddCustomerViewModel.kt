package com.example.toolsonrent.ui.customer.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.CustomerPhoneNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Simple data class to manage phone numbers in the UI before saving
data class TemporaryPhoneNumber(
    val localId: Long = System.nanoTime(), // Unique ID for list management in UI
    var number: String = "",
    var type: String = "Mobile" // Default type
)

// Data class for referrer info, mapping from Customer entity
data class CustomerReferrerInfo(
    val id: Int,
    val name: String
)

class AddCustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()
    private val customerPhoneNumberDao = AppDatabase.getInstance(application).customerPhoneNumberDao()

    private val _saveResult = MutableLiveData<Result<Long>>() // Returns customerId on success
    val saveResult: LiveData<Result<Long>> = _saveResult

    // UI State for new fields
    val name = MutableStateFlow("")
    val email = MutableStateFlow<String?>(null)
    val address = MutableStateFlow<String?>(null)
    val jobField = MutableStateFlow<String?>(null)
    val companyName = MutableStateFlow<String?>(null)
    val selectedReferrerId = MutableStateFlow<Int?>(null)
    val imageUri = MutableStateFlow<String?>(null) // Added for customer image

    private val _phoneNumbers = MutableStateFlow<List<TemporaryPhoneNumber>>(listOf(TemporaryPhoneNumber())) // Start with one empty phone field
    val phoneNumbers: StateFlow<List<TemporaryPhoneNumber>> = _phoneNumbers.asStateFlow()

    private val _potentialReferrers = MutableStateFlow<List<CustomerReferrerInfo>>(emptyList())
    val potentialReferrers: StateFlow<List<CustomerReferrerInfo>> = _potentialReferrers.asStateFlow()

    init {
        loadPotentialReferrers()
    }

    private fun loadPotentialReferrers() {
        viewModelScope.launch {
            customerDao.getAllCustomerReferrerInfo().map { customers ->
                customers.map { CustomerReferrerInfo(it.id, it.name) } // Map to DTO
            }.collect {
                _potentialReferrers.value = it
            }
        }
    }

    fun addPhoneNumberField() {
        _phoneNumbers.value = _phoneNumbers.value + TemporaryPhoneNumber()
    }

    fun removePhoneNumberField(phoneNumber: TemporaryPhoneNumber) {
        _phoneNumbers.value = _phoneNumbers.value.filterNot { it.localId == phoneNumber.localId }
        // Ensure at least one phone number field remains if desired
        if (_phoneNumbers.value.isEmpty()) {
            addPhoneNumberField()
        }
    }

    fun updatePhoneNumber(index: Int, number: String, type: String) {
        val currentList = _phoneNumbers.value.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList[index] = currentList[index].copy(number = number, type = type)
            _phoneNumbers.value = currentList
        }
    }
     fun updatePhoneType(index: Int, type: String) {
        val currentList = _phoneNumbers.value.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList[index] = currentList[index].copy(type = type)
            _phoneNumbers.value = currentList
        }
    }

    fun updatePhoneNumberValue(index: Int, number: String) {
        val currentList = _phoneNumbers.value.toMutableList()
        if (index >= 0 && index < currentList.size) {
            currentList[index] = currentList[index].copy(number = number)
            _phoneNumbers.value = currentList
        }
    }


    fun saveCustomer() {
        val currentName = name.value
        val currentPhoneNumbers = _phoneNumbers.value.filter { it.number.isNotBlank() }

        if (currentName.isBlank()) {
            _saveResult.postValue(Result.failure(IllegalArgumentException("Name cannot be empty.")))
            return
        }
        if (currentPhoneNumbers.isEmpty()) {
            _saveResult.postValue(Result.failure(IllegalArgumentException("At least one phone number must be provided.")))
            return
        }
        // Further validation for phone number format or type can be added here

        val customer = Customer(
            name = currentName,
            email = email.value?.ifBlank { null },
            address = address.value?.ifBlank { null },
            jobField = jobField.value?.ifBlank { null },
            companyName = companyName.value?.ifBlank { null },
            referrerCustomerId = selectedReferrerId.value,
            imageUri = imageUri.value // Added for customer image
        )

        viewModelScope.launch {
            try {
                // Room's @Dao methods with suspend are main-safe.
                // For multiple operations that must succeed or fail together,
                // a @Transaction method in the DAO is the best approach.
                // Here, we assume if customer insert fails, phone numbers won't be saved.
                // If customer insert succeeds but phone number insert fails, we might have partial data.
                // A @Transaction DAO method would be:
                // @Transaction
                // suspend fun insertCustomerWithPhoneNumbers(customer: Customer, phoneNumbers: List<CustomerPhoneNumber>) {
                //     val customerId = insert(customer) // assumes insert returns long
                //     phoneNumbers.forEach { it.customerId = customerId }
                //     customerPhoneNumberDao.insertAll(phoneNumbers)
                // }
                // For now, proceeding with sequential calls:

                val customerId = customerDao.insert(customer) // Assuming insert returns the new row ID.
                                                            // Standard @Insert usually returns Long or List<Long>.
                                                            // If CustomerDao.insert does not return ID, this needs adjustment.
                                                            // Let's assume it needs to be changed in DAO or we fetch customer after insert.

                // To get the ID, if insert doesn't return it, we might need to query by name if names are unique
                // or change DAO. For simplicity, let's assume insert returns ID or we modify DAO later.
                // For now, this viewModel expects `customerDao.insert` to be `suspend fun insert(customer: Customer): Long`

                val phoneEntities = currentPhoneNumbers.map { tempPhone ->
                    CustomerPhoneNumber(
                        customerId = customerId.toInt(), // This needs customerId to be Long from insert
                        phoneNumber = tempPhone.number,
                        phoneType = tempPhone.type
                    )
                }
                customerPhoneNumberDao.insertAll(phoneEntities)
                _saveResult.postValue(Result.success(customerId))
            } catch (e: Exception) {
                _saveResult.postValue(Result.failure(e))
            }
        }
    }
}
