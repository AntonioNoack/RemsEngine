package me.anno.tests.animation

import me.anno.animation.LoopingState
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class LoopingStateTests {
    @Test
    fun testLoopingStates() {
        val times = doubleArrayOf(
            -1.0, 0.0, 1.0, 2.0, 3.0, 4.0,
            5.0, 6.0, 7.0, 8.0
        )
        testLoopingState(
            LoopingState.PLAY_ONCE, 2.5,
            times, doubleArrayOf(0.0, 0.0, 1.0, 2.0, 2.5, 2.5, 2.5, 2.5, 2.5, 2.5)
        )
        testLoopingState(
            LoopingState.PLAY_LOOP, 2.5,
            times, doubleArrayOf(1.5, 0.0, 1.0, 2.0, 0.5, 1.5, 0.0, 1.0, 2.0, 0.5)
        )
        testLoopingState(
            LoopingState.PLAY_REVERSING_LOOP, 2.5,
            times, doubleArrayOf(1.0, 0.0, 1.0, 2.0, 2.0, 1.0, 0.0, 1.0, 2.0, 2.0)
        )
    }

    fun testLoopingState(state: LoopingState, duration: Double, inputs: DoubleArray, outputs: DoubleArray) {
        assertEquals(inputs.size, outputs.size)
        for (i in inputs.indices) {
            assertEquals(outputs[i], state[inputs[i], duration]) {
                "$state[${inputs[i]},$duration]: ${state[inputs[i], duration]} != ${outputs[i]}"
            }
            assertEquals(outputs[i].toFloat(), state[inputs[i].toFloat(), duration.toFloat()])
        }
        val multiplier = 16
        val durationI = (duration * multiplier).toLong()
        for (i in inputs.indices) {
            val inputI = (inputs[i] * multiplier).toLong()
            val outputI = (outputs[i] * multiplier).toLong()
            assertEquals(outputI, state[inputI, durationI]) {
                "$state[${inputI},$durationI]: ${state[inputI, durationI]} != $outputI"
            }
            assertEquals(outputI.toInt(), state[inputI.toInt(), durationI.toInt()])
        }
    }

    @Test
    fun testGetStateById() {
        assertEquals(LoopingState.getById(-1), LoopingState.PLAY_ONCE)
        assertEquals(LoopingState.getById(0), LoopingState.PLAY_ONCE)
        assertEquals(LoopingState.getById(1), LoopingState.PLAY_LOOP)
        assertEquals(LoopingState.getById(2), LoopingState.PLAY_REVERSING_LOOP)
        assertEquals(LoopingState.getById(3), LoopingState.PLAY_ONCE)
    }
}