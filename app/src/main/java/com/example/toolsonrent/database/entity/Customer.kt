package com.example.toolsonrent.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [Index(value = ["referrerCustomerId"])] // Index for faster lookups of referred customers
)
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    // val phoneNumber: String, // REMOVED - Will be replaced by CustomerPhoneNumber entity
    val email: String?,
    val address: String?,
    val jobField: String?,
    val companyName: String?,
    @ForeignKey(
        entity = Customer::class,
        parentColumns = ["id"],
        childColumns = ["referrerCustomerId"],
        onDelete = ForeignKey.SET_NULL // If referrer is deleted, set this field to null
    )
    val referrerCustomerId: Int?,
    val imageUri: String? = null // Added for customer image
)
