package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.toolsonrent.database.entity.Tool
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tool: Tool)

    @Update
    suspend fun update(tool: Tool)

    @Delete
    suspend fun delete(tool: Tool)

    @Query("SELECT * FROM tools ORDER BY name ASC")
    fun getAllTools(): Flow<List<Tool>>

    @Query("SELECT * FROM tools WHERE id = :toolId")
    fun getToolById(toolId: Int): Flow<Tool?>

    // Updated to reflect quantity-based availability
    @Query("SELECT * FROM tools WHERE currentAvailableQuantity > 0 ORDER BY name ASC")
    fun getAvailableTools(): Flow<List<Tool>>

    /**
     * Decrements the currentAvailableQuantity of a tool by the given count.
     * This operation will only succeed if currentAvailableQuantity >= count.
     * Consider checking the return value (number of rows updated) in ViewModel/Repository
     * to confirm if the decrement was successful.
     */
    @Query("UPDATE tools SET currentAvailableQuantity = currentAvailableQuantity - :count WHERE id = :toolId AND currentAvailableQuantity >= :count")
    suspend fun decrementAvailableQuantity(toolId: Int, count: Int = 1): Int // Returns number of rows updated

    /**
     * Increments the currentAvailableQuantity of a tool by the given count.
     * This operation will only succeed if currentAvailableQuantity + count <= totalQuantity.
     * Consider checking the return value (number of rows updated) in ViewModel/Repository
     * to confirm if the increment was successful.
     */
    @Query("UPDATE tools SET currentAvailableQuantity = currentAvailableQuantity + :count WHERE id = :toolId AND currentAvailableQuantity + :count <= totalQuantity")
    suspend fun incrementAvailableQuantity(toolId: Int, count: Int = 1): Int // Returns number of rows updated

    // Old count methods based on isAvailable are removed.
    // New count logic for dashboard metrics will be handled by ViewModels or new specific DAO queries if needed.
    // For example, DashboardViewModel might sum quantities from the Tool list or use specific SUM() queries.
}
