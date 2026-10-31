package com.streaks.habit

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String = "✅",
    val colorArgb: Long = 0xFF2E9BDE,
    val sortOrder: Int = 0
)

/** One (habit, day) completion. Presence of a row = that day was done. */
@Entity(
    tableName = "completion",
    primaryKeys = ["habitId", "dayKey"],
    foreignKeys = [ForeignKey(
        entity = Habit::class, parentColumns = ["id"], childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class Completion(
    val habitId: Long,
    val dayKey: String   // "yyyy-MM-dd"
)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit ORDER BY sortOrder, id")
    fun habits(): Flow<List<Habit>>

    @Insert suspend fun addHabit(h: Habit): Long
    @Delete suspend fun deleteHabit(h: Habit)
    @Query("SELECT COUNT(*) FROM habit") suspend fun habitCount(): Int

    @Query("SELECT dayKey FROM completion WHERE habitId = :habitId")
    fun daysFor(habitId: Long): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM completion WHERE habitId = :habitId AND dayKey = :dayKey)")
    suspend fun isDone(habitId: Long, dayKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markDone(c: Completion)

    @Query("DELETE FROM completion WHERE habitId = :habitId AND dayKey = :dayKey")
    suspend fun unmark(habitId: Long, dayKey: String)
}

@Database(entities = [Habit::class, Completion::class], version = 1, exportSchema = false)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun dao(): HabitDao
    companion object {
        @Volatile private var I: HabitDatabase? = null
        fun get(ctx: android.content.Context): HabitDatabase = I ?: synchronized(this) {
            I ?: Room.databaseBuilder(ctx.applicationContext, HabitDatabase::class.java, "streaks.db")
                .build().also { I = it }
        }
    }
}

/** Streak math — the core logic, kept pure and unit-testable. */
object Streaks {
    private val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Consecutive days done ending today (or yesterday, so a streak survives until end of day). */
    fun current(doneDays: Set<String>, today: LocalDate = LocalDate.now()): Int {
        var day = if (doneDays.contains(today.format(fmt))) today else today.minusDays(1)
        var streak = 0
        while (doneDays.contains(day.format(fmt))) { streak++; day = day.minusDays(1) }
        return streak
    }

    /** Longest run of consecutive days ever. */
    fun best(doneDays: Set<String>): Int {
        if (doneDays.isEmpty()) return 0
        val dates = doneDays.mapNotNull { runCatching { LocalDate.parse(it, fmt) }.getOrNull() }.sorted()
        var best = 1; var run = 1
        for (i in 1 until dates.size) {
            run = if (dates[i - 1].plusDays(1) == dates[i]) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }
}
