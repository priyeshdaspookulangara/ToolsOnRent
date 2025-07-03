package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.toolsonrent.database.entity.Customer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)

    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Int): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE referrerCustomerId = :referrerId ORDER BY name ASC")
    fun getCustomersByReferrer(referrerId: Int): Flow<List<Customer>>

    // Used for populating a list of potential referrers in the UI
    // We only need id and name for this purpose to keep it lightweight.
    // A data class `CustomerReferrerInfo` might be defined for this.
    // For now, returning Flow<List<Customer>> and letting ViewModel map it.
    @Query("SELECT id, name FROM customers ORDER BY name ASC")
    fun getAllCustomerReferrerInfo(): Flow<List<Customer>> // Consider a more specific DTO later if needed

    // Query to search customers by name for referrer selection
    @Query("SELECT id, name FROM customers WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustomersByName(query: String): Flow<List<Customer>> // Consider a DTO
}
