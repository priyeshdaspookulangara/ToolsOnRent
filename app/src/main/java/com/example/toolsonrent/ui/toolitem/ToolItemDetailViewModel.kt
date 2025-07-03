package com.example.toolsonrent.ui.toolitem

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.dao.ToolItemDao
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

// Data class to hold combined ToolItem and its parent Tool (type) information
data class ToolItemWithDetails(
    val toolItem: ToolItem,
    val toolType: Tool? // Nullable if tool type somehow not found, though unlikely with FK
)

@OptIn(ExperimentalCoroutinesApi::class)
class ToolItemDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val toolItemDao: ToolItemDao = AppDatabase.getInstance(application).toolItemDao()
    private val toolDao: ToolDao = AppDatabase.getInstance(application).toolDao()

    private val toolItemId: StateFlow<Int?> = savedStateHandle.getStateFlow("toolItemId", null)

    val toolItemDetails: StateFlow<ToolItemWithDetails?> = toolItemId
        .filterNotNull() // Proceed only if toolItemId is not null
        .flatMapLatest { id ->
            toolItemDao.getItemById(id).flatMapLatest { toolItem ->
                if (toolItem != null) {
                    toolDao.getToolById(toolItem.toolTypeId).map { toolType ->
                        ToolItemWithDetails(toolItem, toolType)
                    }
                } else {
                    flowOf(null) // ToolItem not found
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null // Initial value until data is loaded
        )
}
