package com.example.data.repository

import com.example.data.db.SpeedTestDao
import com.example.data.model.SpeedTestRecord
import kotlinx.coroutines.flow.Flow

class SpeedTestRepository(private val dao: SpeedTestDao) {
    val allRecords: Flow<List<SpeedTestRecord>> = dao.getAllRecords()

    suspend fun insertRecord(record: SpeedTestRecord): Long {
        return dao.insertRecord(record)
    }

    suspend fun deleteRecord(id: Int) {
        dao.deleteRecordById(id)
    }

    suspend fun clearAll() {
        dao.clearAllRecords()
    }
}
