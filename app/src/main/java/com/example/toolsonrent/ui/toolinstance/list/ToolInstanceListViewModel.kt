package com.example.toolsonrent.ui.toolinstance.list

import android.app.Application
import androidx.lifecycle.*
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ToolInstanceListViewModel(
    application: Application,
    val toolTypeId: Int // Made public val to be accessible from Fragment if needed for nav args
) : AndroidViewModel(application) {

    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao()
    private val toolDao = AppDatabase.getInstance(application).toolDao() // To get tool name

    val toolInstances: LiveData<List<ToolInstance>> =
        toolInstanceDao.getInstancesForToolType(toolTypeId).asLiveData()

    // To display the name of the tool type on this screen
    val tool: LiveData<Tool?> = toolDao.getToolById(toolTypeId).asLiveData()


    // Example: If you need to expose counts directly from ViewModel
    val totalInstanceCount: LiveData<Int> =
        toolInstanceDao.getTotalInstanceCountForToolType(toolTypeId).asLiveData()

    val availableInstanceCount: LiveData<Int> =
        toolInstanceDao.getInstanceCountByStatusForToolType(toolTypeId, "Available").asLiveData() // Assuming "Available" is a status string

}

class ToolInstanceListViewModelFactory(
    private val application: Application,
    private val toolTypeId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ToolInstanceListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ToolInstanceListViewModel(application, toolTypeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
