package com.waterbud.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Aggregated total for a single day. */
data class DayTotal(val dayKey: String, val totalHydration: Int)

@Dao
interface WaterDao {

    @Insert
    suspend fun insertLog(log: WaterLog): Long

    @Delete
    suspend fun deleteLog(log: WaterLog)

    @Query("SELECT * FROM water_log WHERE dayKey = :dayKey ORDER BY timestamp DESC")
    fun logsForDay(dayKey: String): Flow<List<WaterLog>>

    @Query("SELECT COALESCE(SUM(hydrationMl), 0) FROM water_log WHERE dayKey = :dayKey")
    fun totalForDay(dayKey: String): Flow<Int>

    /** Daily totals in a date range (inclusive), for charts. */
    @Query(
        "SELECT dayKey, COALESCE(SUM(hydrationMl),0) AS totalHydration FROM water_log " +
            "WHERE dayKey BETWEEN :from AND :to GROUP BY dayKey ORDER BY dayKey ASC"
    )
    fun dailyTotals(from: String, to: String): Flow<List<DayTotal>>

    @Query("DELETE FROM water_log WHERE dayKey = :dayKey")
    suspend fun clearDay(dayKey: String)

    @Query("SELECT * FROM water_log ORDER BY timestamp ASC")
    suspend fun allLogs(): List<WaterLog>

    // --- Aggregates for achievements ---
    @Query(
        "SELECT dayKey, COALESCE(SUM(hydrationMl),0) AS totalHydration FROM water_log " +
            "GROUP BY dayKey ORDER BY dayKey ASC"
    )
    fun allDailyTotals(): Flow<List<DayTotal>>

    @Query("SELECT COUNT(*) FROM water_log")
    fun logCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(hydrationMl),0) FROM water_log")
    fun totalVolume(): Flow<Int>

    // --- Cups ---
    @Query("SELECT * FROM cup ORDER BY sortOrder ASC, id ASC")
    fun cups(): Flow<List<Cup>>

    @Upsert
    suspend fun upsertCup(cup: Cup)

    @Delete
    suspend fun deleteCup(cup: Cup)

    @Query("SELECT COUNT(*) FROM cup")
    suspend fun cupCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCups(cups: List<Cup>)

    // --- Backup / restore ---
    @Query("DELETE FROM water_log")
    suspend fun clearAllLogs()

    @Query("DELETE FROM cup")
    suspend fun clearAllCups()

    @Insert
    suspend fun insertLogs(logs: List<WaterLog>)
}
