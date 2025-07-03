package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tools")
data class Tool(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String?,
    val rentalPrice: Double, // Default rental price for items of this type
    // totalQuantity and currentAvailableQuantity are removed.
    // This information will now be derived from querying ToolItems.
    val imageUri: String? // Represents the generic image for this tool type
)
