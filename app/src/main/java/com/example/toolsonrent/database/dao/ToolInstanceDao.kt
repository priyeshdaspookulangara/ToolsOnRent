package com.example.toolsonrent.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.Delete
import androidx.room.Query
import com.example.toolsonrent.database.entity.ToolInstance
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolInstanceDao {

    @Insert
    suspend fun insert(toolInstance: ToolInstance): Long // Return Long for the new rowId

    @Update
    suspend fun update(toolInstance: ToolInstance)

    @Delete
    suspend fun delete(toolInstance: ToolInstance)

    @Query("SELECT * FROM tool_instances WHERE tool_type_id = :toolTypeId ORDER BY serialNumber ASC")
    fun getInstancesForToolType(toolTypeId: Int): Flow<List<ToolInstance>>

    @Query("SELECT * FROM tool_instances WHERE instanceId = :instanceId")
    fun getInstanceById(instanceId: Int): Flow<ToolInstance?>

    @Query("SELECT COUNT(*) FROM tool_instances WHERE tool_type_id = :toolTypeId")
    fun getTotalInstanceCountForToolType(toolTypeId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM tool_instances WHERE tool_type_id = :toolTypeId AND status = :status")
    fun getInstanceCountByStatusForToolType(toolTypeId: Int, status: String): Flow<Int>

    // Potentially useful query: Get all instances regardless of tool type
    @Query("SELECT * FROM tool_instances ORDER BY tool_type_id, serialNumber ASC")
    fun getAllInstances(): Flow<List<ToolInstance>>

    // Potentially useful for checking serial number uniqueness within a tool type
    @Query("SELECT * FROM tool_instances WHERE tool_type_id = :toolTypeId AND serialNumber = :serialNumber LIMIT 1")
    suspend fun getInstanceBySerialNumber(toolTypeId: Int, serialNumber: String): ToolInstance?
}
