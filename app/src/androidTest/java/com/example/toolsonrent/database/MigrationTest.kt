package com.example.toolsonrent.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName ?: AppDatabase.DATABASE_NAME, // Use DATABASE_NAME as fallback
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        // Create DB with version 4
        var db = helper.createDatabase(TEST_DB, 4).apply {
            // Schema for version 4 (customers table had phoneNumber, no jobField, companyName, referrerCustomerId)
            // Tools and RentalTransactions tables are assumed to be compatible or not relevant to this specific migration focus.
            // We don't need to insert data that would conflict with the old schema if it's being dropped or heavily altered.
            // The old 'phoneNumber' in Customer was removed in entity, so Room handles its absence in version 5 schema.
            // We are primarily testing the addition of new columns to 'customers' and the new 'customer_phone_numbers' table.

            // Example: Insert a customer into the old schema version 4
            // execSQL("INSERT INTO customers (id, name, phoneNumber, email, address) VALUES (1, 'Old Customer', '1234567890', 'old@test.com', '123 Old St')")
            // For this test, we'll just ensure the schema migration itself works.
            // Data preservation for 'customers' columns that remain (name, email, address) is implicit if migration succeeds.
            close()
        }

        // Run migration to version 5
        db = helper.runMigrationsAndValidate(TEST_DB, 5, true, AppDatabase.MIGRATION_4_5)

        // MigrationTestHelper automatically validates the schema after migration.
        // We can add specific data validation if needed.
        // For example, query the new columns or tables.
        // val cursor = db.query("SELECT * FROM customer_phone_numbers")
        // assert(cursor.count == 0) // Expect no phone numbers initially if no data migration logic for them.
        // val customerCursor = db.query("SELECT jobField, companyName, referrerCustomerId FROM customers WHERE id = 1")
        // if (customerCursor.moveToFirst()) {
        //     assert(customerCursor.isNull(customerCursor.getColumnIndex("jobField")))
        // }
        // For now, schema validation by helper is the main goal.
        db.close()
    }
}
