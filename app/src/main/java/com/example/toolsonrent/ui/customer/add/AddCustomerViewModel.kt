package com.example.toolsonrent.ui.customer.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.launch

class AddCustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()

    private val _saveResult = MutableLiveData<Result<Unit>>()
    val saveResult: LiveData<Result<Unit>> = _saveResult

    fun saveCustomer(name: String, phone: String, email: String?, address: String?) {
        // Basic validation (can be enhanced)
        if (name.isBlank() || phone.isBlank()) {
            _saveResult.postValue(Result.failure(IllegalArgumentException("Name and Phone cannot be empty.")))
            return
        }

        val customer = Customer(
            name = name,
            phoneNumber = phone,
            email = email?.ifBlank { null },
            address = address?.ifBlank { null }
        )

        viewModelScope.launch {
            try {
                customerDao.insert(customer)
                _saveResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                // Log.e("AddCustomerViewModel", "Database error", e) // Optional: log DB errors here
                _saveResult.postValue(Result.failure(e))
            }
        }
    }
}
