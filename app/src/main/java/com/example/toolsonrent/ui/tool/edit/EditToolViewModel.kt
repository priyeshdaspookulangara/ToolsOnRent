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

    // Flow for the toolId obtained from navigation arguments via SavedStateHandle.
    private val toolIdFlow: StateFlow<Int?> = savedStateHandle.getStateFlow("toolId", null)

    // StateFlow to hold the Tool object being edited.
    val tool: StateFlow<Tool?> = toolIdFlow.flatMapLatest { id ->
        if (id != null && id != 0) { // Room auto-generated IDs start from 1. 0 is not a valid ID.
            toolDao.getToolById(id) // This DAO method returns Flow<Tool?>
        } else {
            Log.w("EditToolVM", "Invalid toolId ($id) received.")
            flowOf(null) // Emit null if toolId is invalid or not present.
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null // Initially null until toolId is processed and data is fetched.
    )

    // LiveData for update operation result
    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    // LiveData for delete operation result
    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun updateTool(
        currentToolId: Int,
        name: String,
        description: String?,
        priceStr: String,
        isAvailable: Boolean
    ) {
        val price = priceStr.toDoubleOrNull()
        if (name.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Tool name cannot be empty.")))
            return
        }
        if (price == null || price <= 0) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Enter a valid positive rental price.")))
            return
        }
        if (currentToolId == 0) {
             _updateResult.postValue(Result.failure(IllegalStateException("Invalid Tool ID provided for update.")))
            return
        }

        val existingImageUri = tool.value?.imageUri

        val updatedTool = Tool(
            id = currentToolId,
            name = name,
            description = description?.ifBlank { null },
            rentalPrice = price,
            isAvailable = isAvailable,
            imageUri = existingImageUri
        )

        viewModelScope.launch {
            try {
                toolDao.update(updatedTool)
                _updateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditToolVM", "Error updating tool ID $currentToolId", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteTool() {
        val toolToDelete = tool.value // Get the current tool loaded by the StateFlow

        if (toolToDelete == null) {
            _deleteResult.postValue(Result.failure(IllegalStateException("No tool loaded to delete or tool ID is invalid.")))
            return
        }
        // It's good practice to check the ID from the loaded tool itself before deletion.
        if (toolToDelete.id == 0) {
             _deleteResult.postValue(Result.failure(IllegalStateException("Cannot delete tool with invalid ID (0).")))
            return
        }

        viewModelScope.launch {
            try {
                toolDao.delete(toolToDelete)
                _deleteResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                // The Fragment should ideally check the exception type (e.g., SQLiteConstraintException)
                // to provide a more user-friendly message if deletion is blocked by foreign key constraints.
                Log.e("EditToolVM", "Error deleting tool ${toolToDelete.id}: ${e.message}", e)
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}
