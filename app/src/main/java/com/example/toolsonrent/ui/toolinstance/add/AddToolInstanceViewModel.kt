package com.example.toolsonrent.ui.toolinstance.add

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.launch

class AddToolInstanceViewModel(
    application: Application,
    private val toolTypeId: Int
) : AndroidViewModel(application) {

    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao()

    private val _saveResult = MutableLiveData<Result<Long>>()
    val saveResult: LiveData<Result<Long>> = _saveResult

    // Array of possible statuses - could also come from a resource or a more dynamic source
    val statusOptions: List<String> = listOf("Available", "Maintenance", "Damaged", "Lost")


    fun saveInstance(
        serialNumber: String?,
        status: String,
        purchaseDate: Long?,
        notes: String?,
        imageUri: String?
    ) {
        if (status.isBlank()) {
            _saveResult.postValue(Result.failure(IllegalArgumentException("Status is required.")))
            return
        }

        viewModelScope.launch {
            try {
                // Optional: Check for serial number uniqueness within this tool type
                if (!serialNumber.isNullOrBlank()) {
                    val existingInstance = toolInstanceDao.getInstanceBySerialNumber(toolTypeId, serialNumber)
                    if (existingInstance != null) {
                        _saveResult.postValue(Result.failure(IllegalArgumentException("Serial number '$serialNumber' already exists for this tool type.")))
                        return@launch
                    }
                }

                val newInstance = ToolInstance(
                    toolTypeId = toolTypeId,
                    serialNumber = serialNumber?.trim()?.ifBlank { null },
                    status = status,
                    purchaseDate = purchaseDate,
                    notes = notes?.trim()?.ifBlank { null },
                    instanceImageUri = imageUri
                )
                val newId = toolInstanceDao.insert(newInstance)
                if (newId > 0) {
                    _saveResult.postValue(Result.success(newId))
                } else {
                    _saveResult.postValue(Result.failure(Exception("Failed to save new instance, received ID: $newId")))
                }
            } catch (e: Exception) {
                Log.e("AddToolInstanceVM", "Error saving tool instance", e)
                _saveResult.postValue(Result.failure(e))
            }
        }
    }
}

class AddToolInstanceViewModelFactory(
    private val application: Application,
    private val toolTypeId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddToolInstanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddToolInstanceViewModel(application, toolTypeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
