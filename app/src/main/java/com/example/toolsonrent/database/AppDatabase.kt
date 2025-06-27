package com.example.toolsonrent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // New import
import com.example.toolsonrent.database.converter.DateConverter // New import
import com.example.toolsonrent.database.dao.CustomerDao
import com.example.toolsonrent.database.dao.RentalTransactionDao // New import
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.RentalTransaction // New import
import com.example.toolsonrent.database.entity.Tool

@Database(
    entities = [Tool::class, Customer::class, RentalTransaction::class],
    version = 4, // Incremented from 3 to 4
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao
    abstract fun customerDao(): CustomerDao
    abstract fun rentalTransactionDao(): RentalTransactionDao // New DAO abstract method

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tools_on_rent_database"
                )
                // Add migrations here if/when schema changes.
                // .fallbackToDestructiveMigration() is used for simplicity during development
                // and will clear the database on schema changes. Not for production!
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
