package com.example.toolsonrent.ui.rental.active

import java.util.Date

data class ActiveRentalInfo(
    val transactionId: Int,
    val toolItemId: Int, // ID of the specific ToolItem that is rented
    val unitIdUser: String, // User-facing ID of the item (e.g., "DRILL-001")
    val toolTypeName: String, // Name of the tool type (e.g., "Bosch Cordless Drill")
    val customerName: String,
    val rentalDate: Date,
    val dueDate: Date
    // Potentially add item condition or status if needed for display in the active list
)
