package com.example.toolsonrent.ui.reports.customerhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SelectCustomerForReportViewModel(application: Application) : AndroidViewModel(application) {

    private val customerDao = AppDatabase.getInstance(application).customerDao()

    // Exposes a StateFlow of all customers, ordered by name (as defined in CustomerDao).
    // This flow is observed by the fragment to display the list of customers for selection.
    val allCustomers: StateFlow<List<Customer>> = customerDao.getAllCustomers()
        .stateIn(
            scope = viewModelScope, // The ViewModel's own scope for managing the flow.
            started = SharingStarted.WhileSubscribed(5000L), // Keeps the flow active for 5s after the last collector stops.
                                                             // This helps maintain state across short configuration changes.
            initialValue = emptyList() // Provides an initial empty list before the database emits actual data.
        )
}
