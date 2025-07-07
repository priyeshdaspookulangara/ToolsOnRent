package com.example.toolsonrent.ui.rental.active

import java.util.Date

data class ActiveRentalInfo(
    val transactionId: Int,
    val toolInstanceId: Int, // ID of the specific ToolInstance that was rented
    val toolTypeName: String, // Name of the Tool type (e.g., "Hammer")
    val toolInstanceIdentifier: String, // e.g., "SN: 123" or "ID: 45"
    val customerName: String,
    val rentalDate: Date,
    val dueDate: Date,
    val toolImageUri: String? // Image of the tool type for display
)
