package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.toolsonrent.database.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "tool_items",
    foreignKeys = [
        ForeignKey(
            entity = Tool::class,
            parentColumns = ["id"],
            childColumns = ["toolTypeId"],
            onDelete = ForeignKey.RESTRICT // Prevent deleting a Tool if items exist
        )
    ],
    indices = [
        Index(value = ["toolTypeId"]),
        Index(value = ["unitIdUser"], unique = false) // User-facing ID, considering it might not be globally unique initially
        // If unitIdUser should be globally unique, add unique = true.
        // If unique per toolTypeId, complex unique index or application-level check needed.
        // For now, non-unique index for faster lookups. Uniqueness checks will be in ViewModel.
    ]
)
@TypeConverters(DateConverter::class)
data class ToolItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    var unitIdUser: String, // User-facing, modifiable ID. System will suggest a default.

    val toolTypeId: Int,    // Foreign key to the Tool entity (the "template" or "category")

    var serialNumber: String? = null,
    var assetTag: String? = null,
    var currentLocation: String? = "Warehouse", // Default value
    var status: String = "Available",          // Default value; e.g., "Available", "Rented", "Maintenance", "Retired"
    var condition: String? = "Good",           // Default value; e.g., "New", "Good", "Fair", "Poor"

    var lastServiceDate: Date? = null,
    var purchaseDate: Date? = null,
    var purchasePrice: Double? = null,
    var warrantyExpiryDate: Date? = null,
    var notes: String? = null
)
