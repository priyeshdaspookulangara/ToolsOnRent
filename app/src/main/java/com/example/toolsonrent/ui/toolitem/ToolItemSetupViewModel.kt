package com.example.toolsonrent.ui.toolitem

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.dao.ToolItemDao
import com.example.toolsonrent.database.entity.ToolItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Date

// Temporary data class to hold draft item details in UI before saving
data class TemporaryToolItem(
    val localDraftId: Long = System.nanoTime(), // For UI list key
    var unitIdUser: String = "",
    var serialNumber: String? = null,
    var assetTag: String? = null,
    var currentLocation: String = "Warehouse",
    var status: String = "Available",
    var condition: String = "Good",
    var purchaseDate: Date? = null,
    var purchasePrice: Double? = null,
    var warrantyExpiryDate: Date? = null,
    var notes: String? = null,
    // Not including toolTypeId here as it's fixed for the ViewModel instance
)

class ToolItemSetupViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val toolItemDao: ToolItemDao = AppDatabase.getInstance(application).toolItemDao()
    private val toolDao: ToolDao = AppDatabase.getInstance(application).toolDao()

    val toolTypeId: StateFlow<Int?> = savedStateHandle.getStateFlow("toolTypeId", null)

    private val _toolTypeName = MutableStateFlow<String?>(null)
    val toolTypeName: StateFlow<String?> = _toolTypeName.asStateFlow()

    private val _generatedDraftItems = MutableStateFlow<List<TemporaryToolItem>>(emptyList())
    val generatedDraftItems: StateFlow<List<TemporaryToolItem>> = _generatedDraftItems.asStateFlow()

    private val _saveItemsResult = MutableLiveData<Result<Unit>>()
    val saveItemsResult: LiveData<Result<Unit>> = _saveItemsResult

    private val _generationResult = MutableLiveData<Result<Int>>() // Int for count generated
    val generationResult: LiveData<Result<Int>> = _generationResult


    init {
        viewModelScope.launch {
            toolTypeId.collect { id ->
                if (id != null && id != 0) {
                    val tool = toolDao.getToolById(id).firstOrNull()
                    _toolTypeName.value = tool?.name
                } else {
                    _toolTypeName.value = null
                }
            }
        }
    }

    fun generateDraftItems(count: Int) {
        val currentToolTypeId = toolTypeId.value
        val currentToolName = _toolTypeName.value
        if (currentToolTypeId == null || currentToolTypeId == 0 || count <= 0) {
            _generationResult.postValue(Result.failure(IllegalArgumentException("Invalid tool type or count.")))
            return
        }

        val drafts = mutableListOf<TemporaryToolItem>()
        val baseName = currentToolName?.replace(" ", "")?.take(8)?.uppercase() ?: "ITEM"

        // Simple way to find next sequence number - could be improved by checking DB for last used for this tooltype
        var lastUsedSequence = 0
        // In a real scenario, you might query existing items for currentToolTypeId to find highest sequence.
        // For example: val existingItems = toolItemDao.getItemsByToolType(currentToolTypeId).first()
        // then parse unitIdUser to find max sequence. This is simplified for now.

        for (i in 1..count) {
            lastUsedSequence++
            val defaultUnitId = String.format("%s-%03d", baseName, lastUsedSequence)
            drafts.add(
                TemporaryToolItem(
                    unitIdUser = defaultUnitId,
                    // other fields use defaults from TemporaryToolItem data class
                )
            )
        }
        _generatedDraftItems.value = drafts
        _generationResult.postValue(Result.success(drafts.size))
    }

    fun updateDraftItem(index: Int, updatedDraft: TemporaryToolItem) {
        val currentDrafts = _generatedDraftItems.value.toMutableList()
        if (index >= 0 && index < currentDrafts.size) {
            currentDrafts[index] = updatedDraft
            _generatedDraftItems.value = currentDrafts
        }
    }

    fun addEmptyDraftItem() {
        val currentToolTypeId = toolTypeId.value
        val currentToolName = _toolTypeName.value
        val baseName = currentToolName?.replace(" ", "")?.take(8)?.uppercase() ?: "ITEM"
        val nextSeq = _generatedDraftItems.value.size + 1 // Simplified sequence for manually added
        val defaultUnitId = String.format("%s-%03d-M", baseName, nextSeq) // -M for manual

        _generatedDraftItems.value = _generatedDraftItems.value + TemporaryToolItem(unitIdUser = defaultUnitId)
    }

    fun removeDraftItem(draftItem: TemporaryToolItem) {
        _generatedDraftItems.value = _generatedDraftItems.value.filterNot { it.localDraftId == draftItem.localDraftId }
    }


    fun saveGeneratedItems() {
        val currentToolTypeId = toolTypeId.value
        if (currentToolTypeId == null || currentToolTypeId == 0) {
            _saveItemsResult.postValue(Result.failure(IllegalStateException("Tool Type ID is not set.")))
            return
        }
        val draftsToSave = _generatedDraftItems.value
        if (draftsToSave.isEmpty()) {
            _saveItemsResult.postValue(Result.failure(IllegalArgumentException("No items to save.")))
            return
        }

        // Basic Uniqueness Check for unitIdUser within the batch (more robust check against DB needed for production)
        val distinctUnitIds = draftsToSave.map { it.unitIdUser.trim().uppercase() }.distinct()
        if (distinctUnitIds.size != draftsToSave.size) {
            _saveItemsResult.postValue(Result.failure(IllegalArgumentException("Duplicate Unit IDs found in the batch. Please ensure all Unit IDs are unique.")))
            return
        }
        // TODO: Add DB check for unitIdUser uniqueness against existing items for this toolTypeId.
        // Example: Loop through drafts, for each call toolItemDao.getItemByUnitIdUserAndToolType().firstOrNull()
        // If any returns non-null, then it's a duplicate in DB.

        val toolItemsToInsert = draftsToSave.map { draft ->
            ToolItem(
                toolTypeId = currentToolTypeId,
                unitIdUser = draft.unitIdUser.trim(), // Ensure trimmed
                serialNumber = draft.serialNumber?.ifBlank { null },
                assetTag = draft.assetTag?.ifBlank { null },
                currentLocation = draft.currentLocation,
                status = draft.status,
                condition = draft.condition,
                purchaseDate = draft.purchaseDate,
                purchasePrice = draft.purchasePrice,
                warrantyExpiryDate = draft.warrantyExpiryDate,
                notes = draft.notes?.ifBlank { null }
            )
        }

        viewModelScope.launch {
            try {
                toolItemDao.insertAll(toolItemsToInsert)
                _saveItemsResult.postValue(Result.success(Unit))
                _generatedDraftItems.value = emptyList() // Clear drafts after successful save
            } catch (e: Exception) {
                Log.e("ToolItemSetupVM", "Error saving tool items", e)
                _saveItemsResult.postValue(Result.failure(e))
            }
        }
    }
}
