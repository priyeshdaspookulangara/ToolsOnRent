package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_phone_numbers",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE // If a customer is deleted, their phone numbers are also deleted
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerPhoneNumber(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val phoneNumber: String,
    val phoneType: String // e.g., "Mobile", "Work", "Home"
)
