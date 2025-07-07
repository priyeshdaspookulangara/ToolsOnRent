package com.example.toolsonrent.ui.toollistscreen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.toolsonrent.database.AppDatabase
import com.example.toolsonrent.database.dao.ToolInstanceDao
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.*

class ToolListViewModel(application: Application) : AndroidViewModel(application) {

    private val toolDao = AppDatabase.getInstance(application).toolDao()
    private val toolInstanceDao: ToolInstanceDao = AppDatabase.getInstance(application).toolInstanceDao()

    val allToolsWithCounts: StateFlow<List<ToolWithInstanceCounts>> = toolDao.getAllTools()
        .flatMapLatest { tools ->
            // For each tool, we need to get its instance counts.
            // This can be complex if we want to combine multiple flows efficiently.
            // A simpler approach for now, if tools list isn't excessively large,
            // is to map and then query counts for each.
            // For better performance on large lists, consider a database query that joins or aggregates.
            // Since Room doesn't easily support complex relationships in @Query for this,
            // we'll do it reactively.

            if (tools.isEmpty()) {
                flowOf(emptyList())
            } else {
                // Create a list of flows, each fetching counts for one tool
                val listOfFlows: List<Flow<ToolWithInstanceCounts>> = tools.map { tool ->
                    combine(
                        toolInstanceDao.getTotalInstanceCountForToolType(tool.id),
                        toolInstanceDao.getInstanceCountByStatusForToolType(tool.id, "Available") // Assuming "Available" status
                    ) { totalCount, availableCount ->
                        ToolWithInstanceCounts(
                            tool = tool,
                            totalInstanceCount = totalCount,
                            availableInstanceCount = availableCount
                        )
                    }
                }
                // Combine the list of flows into a single flow that emits a list
                combine(listOfFlows) { arrayOfToolWithInstanceCounts ->
                    arrayOfToolWithInstanceCounts.toList()
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
