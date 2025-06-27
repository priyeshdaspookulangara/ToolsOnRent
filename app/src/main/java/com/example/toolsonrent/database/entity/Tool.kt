package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tools")
data class Tool(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val description: String?,
    val rentalPrice: Double,
    // val isAvailable: Boolean = true, // REMOVED as per plan
    var totalQuantity: Int,             // ADDED - Made var assuming it can be edited
    var currentAvailableQuantity: Int,  // ADDED - Made var as it changes with rentals/returns
    val imageUri: String?
)
