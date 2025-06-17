package com.example.toolsonrent.ui.rental.active

import java.util.Date

data class ActiveRentalInfo(
    val transactionId: Int,
    val toolId: Int, // ID of the rented tool, useful for update operations
    val toolName: String,
    val customerName: String,
    val rentalDate: Date,
    val dueDate: Date
)
