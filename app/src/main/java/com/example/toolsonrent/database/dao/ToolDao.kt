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
}
