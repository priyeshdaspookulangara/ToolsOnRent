package com.example.toolsonrent.database.converter

import androidx.room.TypeConverter
import java.util.Date

object DateConverter { // Using object for singleton usage by Room

    @TypeConverter
    @JvmStatic // Ensures Room can discover these methods, especially in a Kotlin object
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    @JvmStatic // Ensures Room can discover these methods
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
