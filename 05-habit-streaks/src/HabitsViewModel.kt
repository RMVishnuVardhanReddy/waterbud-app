package com.streaks.habit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitRow(
    val habit: Habit,
    val doneToday: Boolean,
    val currentStreak: Int,
    val bestStreak: Int,
    val doneDays: Set<String>
)

/** Generous free limit — basics are never paywalled (our differentiator). */
private const val FREE_HABIT_LIMIT = 7

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = HabitDatabase.get(app).dao()
    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private fun todayKey() = LocalDate.now().format(fmt)

    val rows = dao.habits().flatMapLatest { habits ->
        // Combine each habit with its completion days.
        kotlinx.coroutines.flow.combine(
            habits.map { h -> dao.daysFor(h.id) }.ifEmpty { listOf(kotlinx.coroutines.flow.flowOf(emptyList())) }
        ) { dayLists ->
            habits.mapIndexed { i, h ->
                val days = dayLists[i].toSet()
                HabitRow(
                    habit = h,
                    doneToday = days.contains(todayKey()),
                    currentStreak = Streaks.current(days),
                    bestStreak = Streaks.best(days),
                    doneDays = days
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleToday(habit: Habit, done: Boolean) = viewModelScope.launch {
        val key = todayKey()
        if (done) dao.markDone(Completion(habit.id, key)) else dao.unmark(habit.id, key)
    }

    /** Returns false if the free limit is hit (prompt upgrade); true if added. */
    suspend fun addHabit(name: String, emoji: String, isPremium: Boolean): Boolean {
        if (!isPremium && dao.habitCount() >= FREE_HABIT_LIMIT) return false
        dao.addHabit(Habit(name = name, emoji = emoji))
        return true
    }

    fun delete(habit: Habit) = viewModelScope.launch { dao.deleteHabit(habit) }
}
