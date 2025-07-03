package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.toolsonrent.database.entity.ToolItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(toolItem: ToolItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(toolItems: List<ToolItem>)

    @Update
    suspend fun update(toolItem: ToolItem)

    @Delete
    suspend fun delete(toolItem: ToolItem)

    @Query("SELECT * FROM tool_items WHERE id = :id")
    fun getItemById(id: Int): Flow<ToolItem?>

    @Query("SELECT * FROM tool_items WHERE toolTypeId = :toolTypeId ORDER BY unitIdUser ASC")
    fun getItemsByToolType(toolTypeId: Int): Flow<List<ToolItem>>

    @Query("SELECT * FROM tool_items WHERE toolTypeId = :toolTypeId AND status = 'Available' ORDER BY unitIdUser ASC")
    fun getAvailableItemsByToolType(toolTypeId: Int): Flow<List<ToolItem>>

    @Query("UPDATE tool_items SET status = :newStatus WHERE id = :itemId")
    suspend fun updateItemStatus(itemId: Int, newStatus: String)

    @Query("SELECT * FROM tool_items WHERE unitIdUser = :unitIdUser LIMIT 1")
    fun getItemByUnitIdUser(unitIdUser: String): Flow<ToolItem?>

    // For checking uniqueness of unitIdUser within a specific tool type
    @Query("SELECT * FROM tool_items WHERE unitIdUser = :unitIdUser AND toolTypeId = :toolTypeId LIMIT 1")
    fun getItemByUnitIdUserAndToolType(unitIdUser: String, toolTypeId: Int): Flow<ToolItem?>

    @Query("SELECT COUNT(id) FROM tool_items WHERE toolTypeId = :toolTypeId AND status = 'Available'")
    fun getAvailableItemCountByToolType(toolTypeId: Int): Flow<Int>

    @Query("SELECT * FROM tool_items")
    fun getAllToolItems(): Flow<List<ToolItem>> // Added for ActiveRentalsViewModel combine
}
