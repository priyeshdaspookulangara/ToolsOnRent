package com.example.toolsonrent.ui.reports.toolhistory

import java.util.Date

data class ToolRentalHistoryItem(
    val transactionId: Int,
    val customerName: String, // Name of the customer who rented the tool for this specific transaction
    val rentalDate: Date,
    val dueDate: Date,
    val returnDate: Date?, // Nullable, as the rental might still be active or not yet returned
    val rentalPricePerDay: Double, // Price of the tool per day at the time of this specific transaction
    val status: String // Calculated status, e.g., "Active", "Returned", "Overdue"
)
