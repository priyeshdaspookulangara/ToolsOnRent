package com.example.toolsonrent.ui.customer.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class CustomerListViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()

    val allCustomers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keeps the flow active for 5s after last collector stops
            initialValue = emptyList() // Provides an initial empty list
        )
}
