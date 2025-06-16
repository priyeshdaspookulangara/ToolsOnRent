package com.example.toolsonrent.ui.addtool

import android.app.Application
import androidx.lifecycle.*
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.launch

class AddToolViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()

    private val _saveResult = MutableLiveData<Result<Unit>>()
    val saveResult: LiveData<Result<Unit>> = _saveResult

    fun saveTool(tool: Tool) {
        viewModelScope.launch {
            try {
                toolDao.insert(tool)
                _saveResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                // Consider logging the error e.g., Log.e("AddToolViewModel", "Error saving tool", e)
                _saveResult.postValue(Result.failure(e))
            }
        }
    }
}
