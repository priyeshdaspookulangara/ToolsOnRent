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
import com.example.toolsonrent.database.dao.ToolItemDao // Import new DAO
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.CustomerPhoneNumber
import com.example.toolsonrent.database.entity.RentalTransaction
import com.example.toolsonrent.database.entity.Tool
import com.example.toolsonrent.database.entity.ToolItem // Added new entity
import android.util.Log

@Database(
    entities = [
        Tool::class, Customer::class, RentalTransaction::class, CustomerPhoneNumber::class,
        ToolItem::class
    ],
    version = 7, // Incremented from 6 to 7
    exportSchema = true
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun toolDao(): ToolDao
    abstract fun customerDao(): CustomerDao
    abstract fun rentalTransactionDao(): RentalTransactionDao
    abstract fun customerPhoneNumberDao(): CustomerPhoneNumberDao
    abstract fun toolItemDao(): ToolItemDao

    companion object {
        const val DATABASE_NAME = "tools_on_rent_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 4 to 5 (existing)
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `customer_phone_numbers` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `customerId` INTEGER NOT NULL, `phoneNumber` TEXT NOT NULL, `phoneType` TEXT NOT NULL, FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customer_phone_numbers_customerId` ON `customer_phone_numbers` (`customerId`)")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `jobField` TEXT")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `companyName` TEXT")
                db.execSQL("ALTER TABLE `customers` ADD COLUMN `referrerCustomerId` INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_customers_referrerCustomerId` ON `customers` (`referrerCustomerId`)")
            }
        }

        // Migration from version 5 to 6 (New)
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Delete all records from rental_transactions (USER CONFIRMED DATA LOSS)
                db.execSQL("DELETE FROM rental_transactions")

                // 2. Create the new tool_items table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tool_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `unitIdUser` TEXT NOT NULL,
                        `toolTypeId` INTEGER NOT NULL,
                        `serialNumber` TEXT,
                        `assetTag` TEXT,
                        `currentLocation` TEXT DEFAULT 'Warehouse',
                        `status` TEXT NOT NULL DEFAULT 'Available',
                        `condition` TEXT DEFAULT 'Good',
                        `lastServiceDate` INTEGER,
                        `purchaseDate` INTEGER,
                        `purchasePrice` REAL,
                        `warrantyExpiryDate` INTEGER,
                        `notes` TEXT,
                        FOREIGN KEY(`toolTypeId`) REFERENCES `tools`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tool_items_toolTypeId` ON `tool_items` (`toolTypeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tool_items_unitIdUser` ON `tool_items` (`unitIdUser`)")

                // 3. Modify the tools table: Recreate it without quantity columns
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tools_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT,
                        `rentalPrice` REAL NOT NULL,
                        `imageUri` TEXT
                    )
                """)
                db.execSQL("""
                    INSERT INTO `tools_new` (id, name, description, rentalPrice, imageUri)
                    SELECT id, name, description, rentalPrice, imageUri FROM `tools`
                """)
                db.execSQL("DROP TABLE `tools`")
                db.execSQL("ALTER TABLE `tools_new` RENAME TO `tools`")

                // 4. Modify the rental_transactions table: Recreate it with new FK
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `rental_transactions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `toolItemId` INTEGER NOT NULL,
                        `customerId` INTEGER NOT NULL,
                        `rentalDate` INTEGER NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `returnDate` INTEGER,
                        `rentalPricePerDay` REAL NOT NULL,
                        `notes` TEXT,
                        FOREIGN KEY(`toolItemId`) REFERENCES `tool_items`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`customerId`) REFERENCES `customers`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rental_transactions_new_toolItemId` ON `rental_transactions_new` (`toolItemId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rental_transactions_new_customerId` ON `rental_transactions_new` (`customerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rental_transactions_new_rentalDate` ON `rental_transactions_new` (`rentalDate`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rental_transactions_new_dueDate` ON `rental_transactions_new` (`dueDate`)")

                db.execSQL("DROP TABLE `rental_transactions`")
                db.execSQL("ALTER TABLE `rental_transactions_new` RENAME TO `rental_transactions`")
            }
        }

        // Migration from version 6 to 7 (New)
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tool_items` ADD COLUMN `imageUri` TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7) // Add new migration
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
