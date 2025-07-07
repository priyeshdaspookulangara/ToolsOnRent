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
        // totalQuantityStr: String, // REMOVED - Quantity now managed by instances
        imageUri: String?
    ) {
        val price = priceStr.toDoubleOrNull()
        // val newTotalQuantity = totalQuantityStr.toIntOrNull() // REMOVED
        val oldTool = tool.value // Still useful for getting the original image URI for deletion logic

        // Validations
        if (name.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Tool name cannot be empty.")))
            return
        }
        if (price == null || price <= 0) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Enter a valid positive rental price.")))
            return
        }
        // Quantity validation REMOVED
        // if (newTotalQuantity == null || newTotalQuantity < 0) {
        //     _updateResult.postValue(Result.failure(IllegalArgumentException("Total quantity must be a non-negative number.")))
        //     return
        // }
        if (currentToolId == 0) {
            _updateResult.postValue(Result.failure(IllegalStateException("Invalid Tool ID for update.")))
            return
        }
        if (oldTool == null) {
            // This should ideally not happen if the UI is populated after 'tool' StateFlow emits a non-null value.
            _updateResult.postValue(Result.failure(IllegalStateException("Original tool data not loaded. Cannot process update.")))
            return
        }

        // Logic for quantity calculation REMOVED
        // val itemsRented = oldTool.totalQuantity - oldTool.currentAvailableQuantity
        // if (newTotalQuantity < itemsRented) { ... }
        // val newCurrentAvailableQuantity = newTotalQuantity - itemsRented

        // If imageUri is different from oldTool.imageUri, and oldTool.imageUri was not null,
        // then the old image file should be deleted. This should ideally be done *after* successful DB update.
        // For now, the fragment handles telling ImageFileUtil to delete.
        // The ViewModel could also manage this by comparing imageUri with oldTool.imageUri.

        val toolToSave = Tool(
            id = currentToolId,
            name = name,
            description = description?.ifBlank { null },
            rentalPrice = price,
            // totalQuantity and currentAvailableQuantity are no longer part of Tool entity
            imageUri = imageUri
        )

        viewModelScope.launch {
            try {
                // If the image has changed, delete the old one.
                // This is a simplified version; robust handling might involve transactions or post-update cleanup.
                val oldImage = oldTool.imageUri
                if (oldImage != null && oldImage != imageUri) {
                    // It's generally better for the Fragment/View to instruct file deletion
                    // as it has direct access to context for ImageFileUtil.
                    // However, if ViewModel were to do it, it would need context or a helper.
                    // For now, assuming Fragment handles the deletion based on user actions.
                    // If `selectedInternalImageFileUriString` in Fragment is null and `oldImage` was not,
                    // Fragment should have already triggered deletion or will do so.
                    // If `selectedInternalImageFileUriString` is new, Fragment handles deletion of `oldImage`.
                    Log.d("EditToolVM", "Image changed from $oldImage to $imageUri. Fragment should handle old file deletion.")
                }

                toolDao.update(toolToSave)
                _updateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditToolVM", "Error updating tool ID $currentToolId", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteTool() {
        // When deleting a tool type, also delete its image file if it exists
        // And also delete all associated ToolInstance images and ToolInstances (CASCADE should handle instances in DB)
        viewModelScope.launch {
            try {
                val toolToDelete = tool.firstOrNull() // Get the current tool details
                if (toolToDelete != null) {
                    // Delete the main tool image
                    toolToDelete.imageUri?.let {
                        // Again, fragment is better suited for this.
                        // If ViewModel were to do it, it needs more setup.
                        Log.d("EditToolVM", "Tool type ${toolToDelete.id} deleted. Fragment should ensure image $it is deleted.")
                    }
                    // TODO: Add logic here or in Repository to delete all images of associated ToolInstances.
                    // This requires fetching all instances, getting their image URIs, and deleting them.
                    // For now, relying on Fragment/user to manage instance images before deleting type, or handle post-deletion.
                    // The CASCADE delete on ToolInstance table will remove instance records.
                    // We might need a new DAO method to get all image URIs for instances of a tool type.

                    toolDao.delete(toolToDelete)
                    _deleteResult.postValue(Result.success(Unit))
                } else {
                    _deleteResult.postValue(Result.failure(IllegalStateException("Tool to delete not found.")))
                }
            } catch (e: Exception) {
                Log.e("EditToolVM", "Error deleting tool", e)
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}
