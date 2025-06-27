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
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    // Updated: StateFlow for the total count of all tool units (sum of totalQuantity for each tool type).
    val totalToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.sumOf { it.totalQuantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // Updated: StateFlow for the total count of all available tool units (sum of currentAvailableQuantity).
    val availableToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.sumOf { it.currentAvailableQuantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )

    // Updated: StateFlow for the total count of all rented tool units
    // (sum of totalQuantity - currentAvailableQuantity).
    val rentedToolsCount: StateFlow<Int> = allToolsList.map { tools ->
        tools.sumOf { it.totalQuantity - it.currentAvailableQuantity }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0
    )
}
