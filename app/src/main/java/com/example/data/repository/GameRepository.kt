package com.example.data.repository

import com.example.data.db.SimulationDao
import com.example.data.models.SimulationLog
import kotlinx.coroutines.flow.Flow

class GameRepository(private val simulationDao: SimulationDao) {
    val allLogs: Flow<List<SimulationLog>> = simulationDao.getAllLogs()

    suspend fun insertLog(log: SimulationLog) {
        simulationDao.insertLog(log)
    }

    suspend fun clearAllLogs() {
        simulationDao.clearLogs()
    }
}
