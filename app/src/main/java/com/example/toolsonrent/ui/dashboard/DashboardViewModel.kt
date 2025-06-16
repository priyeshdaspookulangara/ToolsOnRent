package com.example.toolsonrent.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    val availableToolsCount: StateFlow<Int> = toolDao.getAvailableToolsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    val rentedToolsCount: StateFlow<Int> = toolDao.getRentedToolsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0
        )

    // Placeholder for other metrics like total tools, overdue tools, daily/monthly profit
    // val totalToolsCount: StateFlow<Int> = toolDao.getTotalToolsCount().stateIn(...)
}
