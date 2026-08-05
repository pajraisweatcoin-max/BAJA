package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SpeedTestRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeedTestDao {
    @Query("SELECT * FROM speed_test_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SpeedTestRecord>>

    @Query("SELECT * FROM speed_test_records WHERE id = :id")
    suspend fun getRecordById(id: Int): SpeedTestRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SpeedTestRecord): Long

    @Query("DELETE FROM speed_test_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM speed_test_records")
    suspend fun clearAllRecords()
}
