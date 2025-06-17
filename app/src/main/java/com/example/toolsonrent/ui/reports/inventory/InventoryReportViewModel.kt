package com.example.toolsonrent.ui.reports.inventory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class InventoryReportViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    // StateFlow for the complete list of tools, ordered by name (as per ToolDao's getAllTools).
    val allToolsList: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keeps flow active for 5s after last collector stops.
            initialValue = emptyList() // Initial value before data is loaded.
        )

    // StateFlow for the total count of tools, derived from allToolsList.
    val totalToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.size
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // StateFlow for the count of available tools, derived from allToolsList.
    val availableToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.count { it.isAvailable } // Counts tools where isAvailable is true.
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // StateFlow for the count of rented (unavailable) tools, derived from allToolsList.
    val rentedToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.count { !it.isAvailable } // Counts tools where isAvailable is false.
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )
}
