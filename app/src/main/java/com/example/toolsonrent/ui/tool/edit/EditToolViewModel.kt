package com.example.toolsonrent.ui.tool.edit

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest usage
class EditToolViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle // Injected by ViewModelProvider
) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    private val toolIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("toolId", null)

    val tool: StateFlow<Tool?> = toolIdFlow.flatMapLatest { id ->
        if (id != null && id != 0) {
            toolDao.getToolById(id)
        } else {
            Log.w("EditToolVM", "Invalid toolId ($id) received.")
            flowOf(null)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun updateTool(
        currentToolId: Int,
        name: String,
        description: String?,
        priceStr: String,
        // totalQuantityStr: String, // Removed
        imageUri: String?
    ) {
        val price = priceStr.toDoubleOrNull()
        // val oldTool = tool.value // Not needed for quantity anymore

        // Validations
        if (name.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Tool name cannot be empty.")))
            return
        }
        if (price == null || price <= 0) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Enter a valid positive rental price.")))
            return
        }
        // Quantity related validations removed

        if (currentToolId == 0) {
             _updateResult.postValue(Result.failure(IllegalStateException("Invalid Tool ID for update.")))
            return
        }
        // if (oldTool == null) { // This check might still be relevant if other fields from oldTool were used
        //     _updateResult.postValue(Result.failure(IllegalStateException("Original tool data not loaded. Cannot process update.")))
        //     return
        // }

        // Quantity calculation logic removed

        val toolToSave = Tool(
            id = currentToolId,
            name = name,
            description = description?.ifBlank { null },
            rentalPrice = price,
            // totalQuantity and currentAvailableQuantity removed
            imageUri = imageUri
        )

        viewModelScope.launch {
            try {
                toolDao.update(toolToSave)
                _updateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditToolVM", "Error updating tool ID $currentToolId", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteTool() { /* ... (existing implementation from previous step) ... */ }
}
