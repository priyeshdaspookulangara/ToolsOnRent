package com.example.toolsonrent.ui.toollistscreen

import com.example.toolsonrent.database.entity.Tool

data class ToolWithInstanceCounts(
    val tool: Tool,
    val totalInstanceCount: Int,
    val availableInstanceCount: Int
)
