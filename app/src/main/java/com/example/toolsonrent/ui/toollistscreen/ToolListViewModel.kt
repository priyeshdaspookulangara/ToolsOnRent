package com.example.toolsonrent.ui.toollistscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ToolListViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    // Expose a StateFlow of the list of tools
    val allTools: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(
            scope = viewModelScope,
            // SharingStarted.Lazily: The upstream flow starts when the first subscriber appears
            // and stops when the scope is cancelled.
            // SharingStarted.WhileSubscribed(5000): More robust, keeps upstream flow active for 5s
            // after the last subscriber disappears, good for handling configuration changes.
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Initial value while the actual data is being loaded.
        )
}
