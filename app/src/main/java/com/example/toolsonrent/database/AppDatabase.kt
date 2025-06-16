package com.example.toolsonrent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.entity.Tool

@Database(entities = [Tool::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao

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
                // Add migrations here if/when schema changes, for now .fallbackToDestructiveMigration() can be used for simplicity during development
                .fallbackToDestructiveMigration() // Not for production!
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
