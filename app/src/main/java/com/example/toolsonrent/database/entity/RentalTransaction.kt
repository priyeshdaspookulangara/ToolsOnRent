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
            entity = Tool::class,
            parentColumns = ["id"],
            childColumns = ["toolId"],
            onDelete = ForeignKey.RESTRICT, // Prevents deleting a Tool if it has rental history
            onUpdate = ForeignKey.CASCADE  // If Tool ID changes, update here
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
        Index(value = ["toolId"]),
        Index(value = ["customerId"]),
        Index(value = ["rentalDate"]),
        Index(value = ["dueDate"])
    ]
)
@TypeConverters(DateConverter::class)
data class RentalTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val toolId: Int,
    val customerId: Int,
    val rentalDate: Date,
    val dueDate: Date,
    var returnDate: Date? = null, // Nullable, as it's set when the tool is returned. Default to null.
    val rentalPricePerDay: Double, // Price of the tool per day at the time of this transaction
    var notes: String? = null // Nullable, can be updated. Default to null.
)
