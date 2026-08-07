package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SshServerDao {
    @Query("SELECT * FROM ssh_servers ORDER BY lastConnectedTime DESC, id DESC")
    fun getAllServers(): Flow<List<SshServerEntity>>

    @Query("SELECT * FROM ssh_servers WHERE id = :id")
    suspend fun getServerById(id: Long): SshServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: SshServerEntity): Long

    @Update
    suspend fun updateServer(server: SshServerEntity)

    @Delete
    suspend fun deleteServer(server: SshServerEntity)

    @Query("UPDATE ssh_servers SET lastConnectedTime = :timestamp WHERE id = :id")
    suspend fun updateLastConnected(id: Long, timestamp: Long)
}
