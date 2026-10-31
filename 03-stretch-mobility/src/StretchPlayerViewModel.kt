package com.unkink.stretch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val stepIndex: Int = 0,
    val secondsLeft: Int = 0,
    val isRunning: Boolean = false,
    val finished: Boolean = false
)

/**
 * The stretch session engine: counts down each step, auto-advances to the next,
 * supports pause/resume/skip/previous, and emits a "cue" when a step starts
 * (hook your TextToSpeech or a beep to [onStepCue]).
 */
class StretchPlayerViewModel(private val routine: Routine) : ViewModel() {

    private val _state = MutableStateFlow(
        PlayerState(secondsLeft = routine.steps.first().seconds, isRunning = true)
    )
    val state = _state.asStateFlow()

    var onStepCue: ((StretchStep) -> Unit)? = null
    var onFinished: (() -> Unit)? = null

    private var ticker: Job? = null

    init { cue(); startTicker() }

    val currentStep get() = routine.steps[_state.value.stepIndex.coerceIn(0, routine.steps.lastIndex)]
    val nextStep get() = routine.steps.getOrNull(_state.value.stepIndex + 1)

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _state.value
                if (!s.isRunning || s.finished) continue
                if (s.secondsLeft > 1) {
                    _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
                } else {
                    advance()
                }
            }
        }
    }

    fun togglePause() = _state.update { it.copy(isRunning = !it.isRunning) }

    fun skip() = advance()

    fun previous() {
        val i = (_state.value.stepIndex - 1).coerceAtLeast(0)
        _state.update { it.copy(stepIndex = i, secondsLeft = routine.steps[i].seconds, finished = false) }
        cue()
    }

    private fun advance() {
        val next = _state.value.stepIndex + 1
        if (next > routine.steps.lastIndex) {
            _state.update { it.copy(isRunning = false, finished = true, secondsLeft = 0) }
            onFinished?.invoke()
        } else {
            _state.update { it.copy(stepIndex = next, secondsLeft = routine.steps[next].seconds) }
            cue()
        }
    }

    private fun cue() { onStepCue?.invoke(currentStep) }

    override fun onCleared() { ticker?.cancel() }
}
