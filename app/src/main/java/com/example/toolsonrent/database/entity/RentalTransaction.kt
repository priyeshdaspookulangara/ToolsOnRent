package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.toolsonrent.database.converter.DateConverter
import java.util.Date

@Entity(
    tableName = "rental_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ToolInstance::class, // Changed from Tool::class
            parentColumns = ["instanceId"],    // Changed from "id"
            childColumns = ["toolInstanceId"], // New field name
            onDelete = ForeignKey.RESTRICT,
            onUpdate = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.RESTRICT, // Prevents deleting a Customer if they have rental history
            onUpdate = ForeignKey.CASCADE  // If Customer ID changes, update here
        )
    ],
    indices = [
        Index(value = ["toolInstanceId"]), // Changed from "toolId"
        Index(value = ["customerId"]),
        Index(value = ["rentalDate"]),
        Index(value = ["dueDate"])
    ]
)
@TypeConverters(DateConverter::class)
data class RentalTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    // val toolId: Int, // REMOVED
    @ColumnInfo(index = true) // Added index explicitly as it's a FK
    val toolInstanceId: Int, // ADDED
    val customerId: Int,
    val rentalDate: Date,
    val dueDate: Date,
    var returnDate: Date? = null, // Nullable, as it's set when the tool is returned. Default to null.
    val rentalPricePerDay: Double, // Price of the tool type per day at the time of this transaction
    var notes: String? = null
)
