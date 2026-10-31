package com.breathe.calm

/** The four phases of a breath cycle. A phase with 0 seconds is skipped. */
enum class Phase(val label: String) {
    INHALE("Breathe in"), HOLD_IN("Hold"), EXHALE("Breathe out"), HOLD_OUT("Hold")
}

/** A breathing pattern: seconds for each phase. */
data class BreathPattern(
    val id: String,
    val name: String,
    val description: String,
    val inhale: Int,
    val holdIn: Int,
    val exhale: Int,
    val holdOut: Int
) {
    val cycleSeconds: Int get() = inhale + holdIn + exhale + holdOut
}

object Patterns {
    val box = BreathPattern("box", "Box breathing", "Calm & focus · 4-4-4-4", 4, 4, 4, 4)
    val relax = BreathPattern("478", "4-7-8", "Fall asleep faster", 4, 7, 8, 0)
    val calm = BreathPattern("calm", "Calm", "Longer exhale to relax", 4, 0, 6, 0)
    val energize = BreathPattern("energize", "Energize", "Wake up gently", 2, 0, 2, 0)
    val all = listOf(box, relax, calm, energize)
}

/**
 * Pure, testable phase calculator. Given the pattern and how many seconds have
 * elapsed in the session, it returns the current phase, the seconds left in it,
 * and a 0..1 "scale" to drive the circle (1 = fully expanded at end of inhale).
 */
class BreathingEngine(private val pattern: BreathPattern) {

    data class Frame(val phase: Phase, val secondsLeftInPhase: Int, val scale: Float, val cycle: Int)

    fun frameAt(elapsedSeconds: Double): Frame {
        val cycleLen = pattern.cycleSeconds.coerceAtLeast(1)
        val cycleIndex = (elapsedSeconds / cycleLen).toInt()
        var t = elapsedSeconds % cycleLen

        val phases = listOf(
            Phase.INHALE to pattern.inhale,
            Phase.HOLD_IN to pattern.holdIn,
            Phase.EXHALE to pattern.exhale,
            Phase.HOLD_OUT to pattern.holdOut
        ).filter { it.second > 0 }

        for ((phase, dur) in phases) {
            if (t < dur) {
                val progress = (t / dur).toFloat()           // 0..1 within this phase
                val scale = when (phase) {
                    Phase.INHALE -> lerp(MIN, MAX, progress)
                    Phase.EXHALE -> lerp(MAX, MIN, progress)
                    Phase.HOLD_IN -> MAX
                    Phase.HOLD_OUT -> MIN
                }
                val left = kotlin.math.ceil(dur - t).toInt().coerceAtLeast(1)
                return Frame(phase, left, scale, cycleIndex)
            }
            t -= dur
        }
        return Frame(Phase.INHALE, pattern.inhale, MIN, cycleIndex)
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    companion object { const val MIN = 0.45f; const val MAX = 1.0f }
}
