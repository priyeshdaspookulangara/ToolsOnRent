package com.example.toolsonrent.ui.toolinstance.edit

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditToolInstanceViewModel(
    application: Application,
    private val instanceId: Int
) : AndroidViewModel(application) {

    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao()

    private val _toolInstance = MutableLiveData<ToolInstance?>()
    val toolInstance: LiveData<ToolInstance?> = _toolInstance

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    // Status options for the spinner, same as in AddToolInstanceViewModel
    val statusOptions: List<String> = listOf("Available", "Rented", "Maintenance", "Damaged", "Lost")

    init {
        loadInstance()
    }

    private fun loadInstance() {
        viewModelScope.launch {
            try {
                _toolInstance.postValue(toolInstanceDao.getInstanceById(instanceId).firstOrNull())
            } catch (e: Exception) {
                Log.e("EditToolInstanceVM", "Error loading tool instance", e)
                _toolInstance.postValue(null) // Signal error or non-existence
            }
        }
    }

    fun updateInstance(
        serialNumber: String?,
        status: String,
        purchaseDate: Long?,
        notes: String?,
        imageUri: String?
    ) {
        val currentInstance = _toolInstance.value
        if (currentInstance == null) {
            _updateResult.postValue(Result.failure(IllegalStateException("Tool instance not loaded or does not exist.")))
            return
        }

        if (status.isBlank()) {
            _updateResult.postValue(Result.failure(IllegalArgumentException("Status is required.")))
            return
        }

        viewModelScope.launch {
            try {
                // Optional: Check for serial number uniqueness if it changed
                val newSerialNumber = serialNumber?.trim()?.ifBlank { null }
                if (newSerialNumber != null && newSerialNumber != currentInstance.serialNumber) {
                    val existingInstanceWithNewSN = toolInstanceDao.getInstanceBySerialNumber(currentInstance.toolTypeId, newSerialNumber)
                    if (existingInstanceWithNewSN != null && existingInstanceWithNewSN.instanceId != currentInstance.instanceId) {
                        _updateResult.postValue(Result.failure(IllegalArgumentException("Serial number '$newSerialNumber' already exists for this tool type.")))
                        return@launch
                    }
                }

                val updatedInstance = currentInstance.copy(
                    serialNumber = newSerialNumber,
                    status = status,
                    purchaseDate = purchaseDate,
                    notes = notes?.trim()?.ifBlank { null },
                    instanceImageUri = imageUri
                )
                toolInstanceDao.update(updatedInstance)
                _updateResult.postValue(Result.success(Unit))
                // Refresh the live data after update
                _toolInstance.postValue(updatedInstance)
            } catch (e: Exception) {
                Log.e("EditToolInstanceVM", "Error updating tool instance", e)
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteInstance() {
        val currentInstance = _toolInstance.value
        if (currentInstance == null) {
            _deleteResult.postValue(Result.failure(IllegalStateException("Tool instance not loaded or does not exist.")))
            return
        }

        viewModelScope.launch {
            try {
                toolInstanceDao.delete(currentInstance)
                _deleteResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("EditToolInstanceVM", "Error deleting tool instance", e)
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}

class EditToolInstanceViewModelFactory(
    private val application: Application,
    private val instanceId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditToolInstanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditToolInstanceViewModel(application, instanceId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
