package com.example.toolsonrent.ui.reports.toolhistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SelectToolForReportViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    // Exposes a StateFlow of all tools, ordered by name (as defined in ToolDao's getAllTools).
    // This is used by the fragment to display a list of tools for selection.
    // For a history report, the user might want to see the history of any tool,
    // regardless of its current availability, so we fetch all tools.
    val allTools: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(
            scope = viewModelScope, // The ViewModel's own scope for managing the flow.
            started = SharingStarted.WhileSubscribed(5000L), // Keeps the flow active for 5s after the last collector stops.
                                                             // This helps maintain state across short configuration changes.
            initialValue = emptyList() // Provides an initial empty list before the database emits actual data.
        )
}
