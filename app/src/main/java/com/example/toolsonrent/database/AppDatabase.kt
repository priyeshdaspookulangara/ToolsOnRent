package com.example.toolsonrent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.toolsonrent.database.converter.DateConverter
import com.example.toolsonrent.database.dao.CustomerDao
import com.example.toolsonrent.database.dao.RentalTransactionDao
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.dao.ToolInstanceDao // Added
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolInstance // Added
import android.util.Log

@Database(
    entities = [Tool::class, Customer::class, RentalTransaction::class, ToolInstance::class], // Added ToolInstance
    version = 5, // Incremented from 4 to 5
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao
    abstract fun customerDao(): CustomerDao
    abstract fun rentalTransactionDao(): RentalTransactionDao
    abstract fun toolInstanceDao(): ToolInstanceDao // Added

    companion object {
        const val DATABASE_NAME = "tools_on_rent_database" // Made public

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME // Use the constant
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

        fun closeInstance() {
            INSTANCE?.let {
                if (it.isOpen) {
                    it.close()
                    Log.i("AppDatabase", "Database instance closed.")
                }
                INSTANCE = null
            }
        }
    }
}
