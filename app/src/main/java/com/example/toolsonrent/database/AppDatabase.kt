package com.example.toolsonrent.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.toolsonrent.database.converter.DateConverter
import com.example.toolsonrent.database.dao.CustomerDao
import com.example.toolsonrent.database.dao.RentalTransactionDao
import com.example.toolsonrent.database.dao.ToolDao
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.CustomerPhoneNumber // Added new entity
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import android.util.Log

@Database(
    entities = [Tool::class, Customer::class, RentalTransaction::class, CustomerPhoneNumber::class], // Added CustomerPhoneNumber
    version = 5, // Incremented from 4 to 5
    exportSchema = true // Recommended to set to true for production to keep schema history
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao
    abstract fun customerDao(): CustomerDao
    abstract fun rentalTransactionDao(): RentalTransactionDao
    abstract fun customerPhoneNumberDao(): CustomerPhoneNumberDao

    companion object {
        const val DATABASE_NAME = "tools_on_rent_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 4 to 5
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new customer_phone_numbers table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `customer_phone_numbers` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `phoneType` TEXT NOT NULL,
                        FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_phone_numbers_customerId` ON `customer_phone_numbers` (`customerId`)")

                // 2. Add new columns to the customers table
                // SQLite does not support adding foreign key constraints directly via ALTER TABLE for existing columns in older versions.
                // We are adding nullable columns first. The FK constraint on referrerCustomerId is defined in the entity for new tables.
                // Room will validate this schema.
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `jobField` TEXT")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `companyName` TEXT")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `referrerCustomerId` INTEGER") // Nullable INTEGER

                // 3. Create an index for referrerCustomerId on the customers table
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_referrerCustomerId` ON `customers` (`referrerCustomerId`)")

                // 4. Data migration for existing phone numbers (optional, but good practice)
                // This is a placeholder. If there was a single `phoneNumber` column to migrate from,
                // we would insert it into the new table here.
                // For now, we assume new phone numbers will be added via UI.
                // Example:
                // db.execSQL("""
                //     INSERT INTO customer_phone_numbers (customerId, phoneNumber, phoneType)
                //     SELECT id, phoneNumber, 'Mobile' FROM customers WHERE phoneNumber IS NOT NULL AND phoneNumber != ''
                // """)
                // After this, the old phoneNumber column in 'customers' table would be dropped if it existed and was migrated.
                // Since it was removed from the entity, Room handles the schema without it.
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_4_5) // Add the migration
                // .fallbackToDestructiveMigration() // Remove this if using migrations
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
