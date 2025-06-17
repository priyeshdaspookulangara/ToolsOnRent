package com.example.toolsonrent.ui.reports.overdue

import java.util.Date

data class OverdueRentalInfo(
    val transactionId: Int,
    val toolName: String,
    val customerName: String,
    val dueDate: Date,
    val daysOverdue: Long // Using Long for days calculation, can be Int if max overdue days is within Int range
)
