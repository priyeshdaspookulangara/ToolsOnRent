package com.example.toolsonrent.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.toolsonrent.database.dao.CustomerDao
import com.example.toolsonrent.database.dao.CustomerPhoneNumberDao
import com.example.toolsonrent.database.entity.Customer
import com.example.toolsonrent.database.entity.CustomerPhoneNumber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class CustomerDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var customerDao: CustomerDao
    private lateinit var customerPhoneNumberDao: CustomerPhoneNumberDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            // Allowing main thread queries, just for testing.
            .allowMainThreadQueries()
            .build()
        customerDao = db.customerDao()
        customerPhoneNumberDao = db.customerPhoneNumberDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetCustomerReferrerInfo() = runBlocking {
        val customer1 = Customer(id = 1, name = "Referrer One", email = "ref1@test.com", address = "1 St", jobField = "Boss", companyName = "Big Co", referrerCustomerId = null)
        val customer2 = Customer(id = 2, name = "Referred One", email = "refd1@test.com", address = "2 St", jobField = "Worker", companyName = "Big Co", referrerCustomerId = 1)
        customerDao.insert(customer1)
        customerDao.insert(customer2)

        val referrersInfo = customerDao.getAllCustomerReferrerInfo().first()
        // This query currently returns full Customer objects, but only id and name are guaranteed if optimized.
        // For this test, we check if both customers are returned and their names are correct.
        assertEquals(2, referrersInfo.size)
        assertTrue(referrersInfo.any { it.name == "Referrer One" && it.id == 1 })
        assertTrue(referrersInfo.any { it.name == "Referred One" && it.id == 2 })
    }

    @Test
    @Throws(Exception::class)
    fun getCustomersByReferrerTest() = runBlocking {
        val referrer = Customer(id = 1, name = "Main Referrer", email = "main@ref.com", address = "Addr", jobField = "JF", companyName = "CN", referrerCustomerId = null)
        val referred1 = Customer(id = 2, name = "First Referred", email = "r1@test.com", address = "Addr1", jobField = "JF", companyName = "CN", referrerCustomerId = 1)
        val referred2 = Customer(id = 3, name = "Second Referred", email = "r2@test.com", address = "Addr2", jobField = "JF", companyName = "CN", referrerCustomerId = 1)
        val unrelated = Customer(id = 4, name = "Unrelated Customer", email = "un@test.com", address = "Addr3", jobField = "JF", companyName = "CN", referrerCustomerId = null)

        customerDao.insert(referrer)
        customerDao.insert(referred1)
        customerDao.insert(referred2)
        customerDao.insert(unrelated)

        val customersOfReferrer1 = customerDao.getCustomersByReferrer(1).first()
        assertEquals(2, customersOfReferrer1.size)
        assertTrue(customersOfReferrer1.any { it.name == "First Referred" })
        assertTrue(customersOfReferrer1.any { it.name == "Second Referred" })
    }
     @Test
    @Throws(Exception::class)
    fun insertCustomerAndPhoneNumbersThenRetrieve() = runBlocking {
        val customer = Customer(name = "John Doe", email = "john@example.com", address = "123 Main St", jobField = "Engineer", companyName = "Tech Corp", referrerCustomerId = null)
        val customerId = customerDao.insert(customer).toInt()

        val phone1 = CustomerPhoneNumber(customerId = customerId, phoneNumber = "555-0101", phoneType = "Mobile")
        val phone2 = CustomerPhoneNumber(customerId = customerId, phoneNumber = "555-0102", phoneType = "Work")
        customerPhoneNumberDao.insertAll(listOf(phone1, phone2))

        val loadedCustomer = customerDao.getCustomerById(customerId).first()
        assertEquals("John Doe", loadedCustomer?.name)
        assertEquals("Engineer", loadedCustomer?.jobField)

        val loadedPhones = customerPhoneNumberDao.getPhoneNumbersForCustomer(customerId).first()
        assertEquals(2, loadedPhones.size)
        assertTrue(loadedPhones.any { it.phoneNumber == "555-0101" && it.phoneType == "Mobile" })
        assertTrue(loadedPhones.any { it.phoneNumber == "555-0102" && it.phoneType == "Work" })
    }
}
