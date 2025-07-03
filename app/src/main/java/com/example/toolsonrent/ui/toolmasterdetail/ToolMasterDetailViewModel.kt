package com.example.toolsonrent.ui.toolmasterdetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.dao.ToolItemDao
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ToolMasterDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao: ToolDao = AppDatabase.getInstance(application).toolDao()
    private val toolItemDao: ToolItemDao = AppDatabase.getInstance(application).toolItemDao()

    val allToolTypes: StateFlow<List<Tool>> = toolDao.getAllTools()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedToolTypeId = MutableStateFlow<Int?>(null)
    val selectedToolTypeId: StateFlow<Int?> = _selectedToolTypeId.asStateFlow()

    // To hold the name of the selected tool type for display in the right pane header
    private val _selectedToolTypeName = MutableStateFlow<String?>(null)
    val selectedToolTypeName: StateFlow<String?> = _selectedToolTypeName.asStateFlow()


    val itemsForSelectedToolType: StateFlow<List<ToolItem>> = _selectedToolTypeId.flatMapLatest { typeId ->
        if (typeId != null && typeId != 0) {
            // Update the selected tool type name
            val selectedTool = allToolTypes.value.find { it.id == typeId }
            _selectedToolTypeName.value = selectedTool?.name
            toolItemDao.getItemsByToolType(typeId)
        } else {
            _selectedToolTypeName.value = null // Clear name if no type is selected
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Added to get available item count for a tool type
    fun getAvailableItemCount(toolTypeId: Int): StateFlow<Int> {
        return toolItemDao.getAvailableItemCountByToolType(toolTypeId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    }


    fun selectToolType(toolTypeId: Int?) {
        _selectedToolTypeId.value = toolTypeId
        if (toolTypeId == null) {
            _selectedToolTypeName.value = null // Clear name if selection is cleared
        }
        // The flatMapLatest on itemsForSelectedToolType will react to this change.
    }
}
