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
import com.example.toolsonrent.database.entity.CustomerPhoneNumber
import com.example.toolsonrent.ui.customer.add.CustomerReferrerInfo // Reusing DTO
import com.example.toolsonrent.ui.customer.add.TemporaryPhoneNumber // Reusing DTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class EditCustomerViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()
    private val customerPhoneNumberDao = AppDatabase.getInstance(application).customerPhoneNumberDao()

    private val customerIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("customerId", null)
    val currentCustomerId = MutableStateFlow<Int?>(null)


    // UI State for fields - using MutableStateFlow to allow two-way binding like updates from UI
    val name = MutableStateFlow("")
    val email = MutableStateFlow<String?>(null)
    val address = MutableStateFlow<String?>(null)
    val jobField = MutableStateFlow<String?>(null)
    val companyName = MutableStateFlow<String?>(null)
    val selectedReferrerId = MutableStateFlow<Int?>(null)

    private val _phoneNumbers = MutableStateFlow<List<TemporaryPhoneNumber>>(emptyList())
    val phoneNumbers: StateFlow<List<TemporaryPhoneNumber>> = _phoneNumbers.asStateFlow()

    private val _potentialReferrers = MutableStateFlow<List<CustomerReferrerInfo>>(emptyList())
    val potentialReferrers: StateFlow<List<CustomerReferrerInfo>> = _potentialReferrers.asStateFlow()

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private val _customerLoaded = MutableLiveData<Boolean>(false)
    val customerLoaded: LiveData<Boolean> = _customerLoaded


    init {
        loadPotentialReferrers()
        observeCustomerIdAndLoadCustomer()
    }

    private fun loadPotentialReferrers() {
        viewModelScope.launch {
            customerDao.getAllCustomerReferrerInfo().map { customers ->
                customers.map { CustomerReferrerInfo(it.id, it.name) }
            }.collect {
                _potentialReferrers.value = it
            }
        }
    }

    private fun observeCustomerIdAndLoadCustomer() {
        viewModelScope.launch {
            customerIdFlow.collectLatest { id ->
                currentCustomerId.value = id
                if (id != null && id != 0) {
                    customerDao.getCustomerById(id).collectLatest { customer ->
                        if (customer != null) {
                            name.value = customer.name
                            email.value = customer.email
                            address.value = customer.address
                            jobField.value = customer.jobField
                            companyName.value = customer.companyName
                            selectedReferrerId.value = customer.referrerCustomerId

                            // Load phone numbers
                            customerPhoneNumberDao.getPhoneNumbersForCustomer(customer.id).collectLatest { phones ->
                                _phoneNumbers.value = phones.map { TemporaryPhoneNumber(number = it.phoneNumber, type = it.phoneType) }
                                if (_phoneNumbers.value.isEmpty()){ // Ensure at least one field if no numbers exist
                                    addPhoneNumberField()
                                }
                                _customerLoaded.postValue(true) // Signal that data is loaded
                            }
                        } else {
                             _customerLoaded.postValue(false) // Not found or error
                            Log.w("EditCustomerVM", "Customer with ID $id not found.")
                        }
                    }
                } else {
                    _customerLoaded.postValue(false) // Invalid ID
                    Log.w("EditCustomerVM", "Invalid customerId ($id) received for editing.")
                }
            }
        }
    }

    fun addPhoneNumberField() {
        _phoneNumbers.value = _phoneNumbers.value + TemporaryPhoneNumber()
    }

    fun removePhoneNumberField(phoneNumber: TemporaryPhoneNumber) {
        _phoneNumbers.value = _phoneNumbers.value.filterNot { it.localId == phoneNumber.localId }
         if (_phoneNumbers.value.isEmpty()) { // Optionally ensure one field remains
            addPhoneNumberField()
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

    fun updateCustomer() {
        val custId = currentCustomerId.value
        if (custId == null || custId == 0) {
            _updateResult.postValue(Result.failure(IllegalStateException("Invalid Customer ID for update.")))
            return
        }

        if (selectedReferrerId.value == custId) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("A customer cannot be their own referrer.")))
            return
        }

        val currentName = name.value
        val currentPhoneNumbers = _phoneNumbers.value.filter { it.number.isNotBlank() }

        if (currentName.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Name cannot be empty.")))
            return
        }
        if (currentPhoneNumbers.isEmpty()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("At least one phone number must be provided.")))
            return
        }

        val updatedCustomer = Customer(
            id = custId,
            name = currentName,
            email = email.value?.ifBlank { null },
            address = address.value?.ifBlank { null },
            jobField = jobField.value?.ifBlank { null },
            companyName = companyName.value?.ifBlank { null },
            referrerCustomerId = selectedReferrerId.value
        )

        viewModelScope.launch {
            try {
                // This should ideally be a single transaction in the DAO
                customerDao.update(updatedCustomer)
                customerPhoneNumberDao.deleteAllForCustomer(custId) // Clear old phones for this customer
                val phoneEntities = currentPhoneNumbers.map { tempPhone ->
                    CustomerPhoneNumber(
                        customerId = custId,
                        phoneNumber = tempPhone.number,
                        phoneType = tempPhone.type
                    )
                }
                customerPhoneNumberDao.insertAll(phoneEntities)
                _updateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditCustomerVM", "Error updating customer ID $custId", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteCustomer() {
        val custId = currentCustomerId.value
        if (custId == null || custId == 0) {
            _deleteResult.postValue(Result.failure(IllegalStateException("No customer loaded or customer ID is invalid.")))
            return
        }
         // Deleting the customer will trigger CASCADE delete for CustomerPhoneNumbers due to FK constraint.
        viewModelScope.launch {
            try {
                // Construct a minimal customer object for deletion, or ensure the DAO's delete takes an ID.
                // If delete takes an entity, it must be the one from the DB or have the correct ID.
                customerDao.delete(Customer(id = custId, name = "", referrerCustomerId = null)) // name/referrerCustomerId not used by delete by ID
                _deleteResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditCustomerVM", "Error deleting customer $custId", e)
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}
