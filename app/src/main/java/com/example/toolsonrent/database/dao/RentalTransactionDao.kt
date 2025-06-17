package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
// No OnConflictStrategy import needed if using default ABORT
import com.example.toolsonrent.database.entity.RentalTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface RentalTransactionDao {

    @Insert // By default, OnConflictStrategy.ABORT is used.
    suspend fun insert(transaction: RentalTransaction): Long // Return new row ID

    @Update
    suspend fun update(transaction: RentalTransaction)

    @Delete
    suspend fun delete(transaction: RentalTransaction)

    @Query("SELECT * FROM rental_transactions WHERE id = :transactionId")
    fun getTransactionById(transactionId: Int): Flow<RentalTransaction?>

    @Query("SELECT * FROM rental_transactions ORDER BY rentalDate DESC")
    fun getAllTransactions(): Flow<List<RentalTransaction>>

    @Query("SELECT * FROM rental_transactions WHERE toolId = :toolId ORDER BY rentalDate DESC")
    fun getTransactionsForTool(toolId: Int): Flow<List<RentalTransaction>>

    @Query("SELECT * FROM rental_transactions WHERE customerId = :customerId ORDER BY rentalDate DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<RentalTransaction>>

    // Query for currently active rentals (tool has not been returned yet)
    @Query("SELECT * FROM rental_transactions WHERE returnDate IS NULL ORDER BY dueDate ASC")
    fun getActiveRentals(): Flow<List<RentalTransaction>>

    // Query for overdue rentals (returnDate is null and dueDate is in the past)
    // We pass 'currentDate' as a Long (timestamp) for comparison, matching how DateConverter stores dates.
    @Query("SELECT * FROM rental_transactions WHERE returnDate IS NULL AND dueDate < :currentDate ORDER BY dueDate ASC")
    fun getOverdueRentals(currentDate: Long): Flow<List<RentalTransaction>>

    @Query("SELECT * FROM rental_transactions WHERE returnDate IS NOT NULL AND rentalDate >= :startDate AND rentalDate <= :endDate ORDER BY rentalDate DESC")
    fun getCompletedTransactionsInRange(startDate: Long, endDate: Long): kotlinx.coroutines.flow.Flow<List<com.example.toolsonrent.database.entity.RentalTransaction>>
}
