package com.example.toolsonrent.ui.reports.customerhistory

import java.util.Date

data class CustomerRentalHistoryItem( // Renamed class
    val transactionId: Int,
    val toolName: String,
    val rentalDate: Date,
    val dueDate: Date,
    val returnDate: Date?,
    val rentalPricePerDay: Double,
    val status: String // Added status field
)
