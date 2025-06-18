package com.example.toolsonrent.ui.dashboard.calendar.details

import java.util.Date

/**
 * Represents a single item in a list of rental details for a specific calendar day.
 * This is used to show users what rentals are due, overdue, or upcoming on a selected date.
 *
 * @property toolName The name of the tool involved in the rental.
 * @property customerName The name of the customer who rented the tool.
 * @property fullDueDate The precise due date and time of the rental.
 * @property status A string indicating the rental's status relative to the selected calendar day
 *                  (e.g., "Due Today", "Overdue", "Upcoming Return").
 * @property transactionId The ID of the underlying rental transaction, useful for navigation or further actions.
 */
data class DailyRentalDetailItem(
    val toolName: String,
    val customerName: String,
    val fullDueDate: Date,
    val status: String,
    val transactionId: Int
)
