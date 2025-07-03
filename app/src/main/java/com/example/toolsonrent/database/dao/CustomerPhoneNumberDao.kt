package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.toolsonrent.database.entity.CustomerPhoneNumber
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerPhoneNumberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customerPhoneNumber: CustomerPhoneNumber): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customerPhoneNumbers: List<CustomerPhoneNumber>)

    @Update
    suspend fun update(customerPhoneNumber: CustomerPhoneNumber)

    @Delete
    suspend fun delete(customerPhoneNumber: CustomerPhoneNumber)

    @Query("DELETE FROM customer_phone_numbers WHERE customerId = :customerId")
    suspend fun deleteAllForCustomer(customerId: Int)

    @Query("SELECT * FROM customer_phone_numbers WHERE customerId = :customerId")
    fun getPhoneNumbersForCustomer(customerId: Int): Flow<List<CustomerPhoneNumber>>

    @Query("SELECT * FROM customer_phone_numbers WHERE id = :id")
    suspend fun getPhoneNumberById(id: Int): CustomerPhoneNumber?
}
