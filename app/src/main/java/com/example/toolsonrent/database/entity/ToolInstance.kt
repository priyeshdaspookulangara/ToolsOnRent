package com.example.toolsonrent.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tool_instances",
    foreignKeys = [ForeignKey(
        entity = Tool::class,
        parentColumns = ["id"],
        childColumns = ["tool_type_id"],
        onDelete = ForeignKey.CASCADE // If a Tool type is deleted, its instances are also deleted.
    )]
)
data class ToolInstance(
    @PrimaryKey(autoGenerate = true)
    val instanceId: Int = 0,

    @ColumnInfo(name = "tool_type_id", index = true)
    val toolTypeId: Int,

    @ColumnInfo(name = "serial_number")
    val serialNumber: String?, // Can be null if not applicable

    @ColumnInfo(name = "status")
    val status: String, // E.g., "Available", "Rented", "Maintenance", "Damaged", "Lost"

    @ColumnInfo(name = "purchase_date")
    val purchaseDate: Long?, // Store as Long (timestamp)

    @ColumnInfo(name = "notes")
    val notes: String?,

    @ColumnInfo(name = "instance_image_uri")
    val instanceImageUri: String?
)
