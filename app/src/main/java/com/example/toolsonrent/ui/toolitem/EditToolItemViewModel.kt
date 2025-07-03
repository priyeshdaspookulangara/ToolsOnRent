package com.example.toolsonrent.ui.toolitem

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolItemDao
import com.example.toolsonrent.database.entity.ToolItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Date

class EditToolItemViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val toolItemDao: ToolItemDao = AppDatabase.getInstance(application).toolItemDao()
    val toolItemId: StateFlow<Int?> = savedStateHandle.getStateFlow("toolItemId", null)

    private var originalToolItem: ToolItem? = null // To compare for changes and for toolTypeId

    // UI State - Using MutableStateFlow for easier two-way binding simulation in Fragment
    val unitIdUser = MutableStateFlow("")
    val serialNumber = MutableStateFlow<String?>(null)
    val assetTag = MutableStateFlow<String?>(null)
    val imageUri = MutableStateFlow<String?>(null)
    val status = MutableStateFlow("Available") // Default, will be overwritten by loaded item
    val condition = MutableStateFlow("Good")   // Default
    val currentLocation = MutableStateFlow("Warehouse") // Default
    val purchasePrice = MutableStateFlow<Double?>(null)
    val purchaseDate = MutableStateFlow<Date?>(null)
    val warrantyExpiryDate = MutableStateFlow<Date?>(null)
    val notes = MutableStateFlow<String?>(null)

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded.asStateFlow()

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    init {
        viewModelScope.launch {
            toolItemId.collectLatest { id ->
                if (id != null && id != 0) {
                    loadToolItem(id)
                } else {
                    _isDataLoaded.value = false
                    // Handle error or invalid state if ID is missing for an edit screen
                }
            }
        }
    }

    private suspend fun loadToolItem(id: Int) {
        val item = toolItemDao.getItemById(id).firstOrNull()
        originalToolItem = item
        if (item != null) {
            unitIdUser.value = item.unitIdUser
            serialNumber.value = item.serialNumber
            assetTag.value = item.assetTag
            imageUri.value = item.imageUri
            status.value = item.status
            condition.value = item.condition ?: "Good"
            currentLocation.value = item.currentLocation ?: "Warehouse"
            purchasePrice.value = item.purchasePrice
            purchaseDate.value = item.purchaseDate
            warrantyExpiryDate.value = item.warrantyExpiryDate
            notes.value = item.notes
            _isDataLoaded.value = true
        } else {
            _isDataLoaded.value = false
            // Post an error or handle item not found
            _updateResult.postValue(Result.failure(Exception("Tool item not found.")))
        }
    }

    fun updateToolItem() {
        val currentItemId = toolItemId.value
        val originalItem = originalToolItem
        if (currentItemId == null || currentItemId == 0 || originalItem == null) {
            _updateResult.postValue(Result.failure(IllegalStateException("Tool item data not loaded or invalid ID.")))
            return
        }

        val newUnitIdUser = unitIdUser.value.trim()
        if (newUnitIdUser.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Unit User ID cannot be empty.")))
            return
        }

        // TODO: Uniqueness check for unitIdUser if it has changed
        // if (newUnitIdUser != originalItem.unitIdUser) {
        //    viewModelScope.launch {
        //        val existing = toolItemDao.getItemByUnitIdUserAndToolType(newUnitIdUser, originalItem.toolTypeId).firstOrNull()
        //        if (existing != null && existing.id != currentItemId) {
        //             _updateResult.postValue(Result.failure(IllegalArgumentException("This Unit User ID is already in use for another item of the same tool type.")))
        //             return@launch // from this inner launch
        //        }
        //        proceedWithUpdate(currentItemId, originalItem.toolTypeId, newUnitIdUser) // Call a helper to continue
        //    }
        //    return // from updateToolItem, actual update happens in proceedWithUpdate
        // }

        // For now, skipping async uniqueness check for brevity in this step
        proceedWithUpdate(currentItemId, originalItem.toolTypeId, newUnitIdUser)
    }

    // Helper to allow async check before this if needed
    private fun proceedWithUpdate(itemId: Int, itemToolTypeId: Int, validatedUnitIdUser: String) {
         val updatedItem = ToolItem(
            id = itemId,
            toolTypeId = itemToolTypeId, // From original item, not editable here
            unitIdUser = validatedUnitIdUser,
            serialNumber = serialNumber.value?.ifBlank { null },
            assetTag = assetTag.value?.ifBlank { null },
            imageUri = imageUri.value,
            status = status.value,
            condition = condition.value,
            currentLocation = currentLocation.value,
            purchasePrice = purchasePrice.value,
            purchaseDate = purchaseDate.value,
            warrantyExpiryDate = warrantyExpiryDate.value,
            notes = notes.value?.ifBlank { null }
        )

        viewModelScope.launch {
            try {
                toolItemDao.update(updatedItem)
                _updateResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditToolItemVM", "Error updating tool item $itemId", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }
}
