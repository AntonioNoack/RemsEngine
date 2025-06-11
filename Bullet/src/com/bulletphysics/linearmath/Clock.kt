package com.bulletphysics.linearmath

/**
 * Clock is a portable basic clock that measures accurate time in seconds, use for profiling.
 *
 * @author jezek2
 */
class Clock {

    private var startTime: Long = 0

    /**
     * Creates a new clock and resets it.
     */
    init {
        reset()
    }

    /**
     * Resets clock by setting start time to current.
     */
    fun reset() {
        startTime = System.nanoTime()
    }

    /**
     * Returns the time in nanoseconds since the last call to reset or since the Clock was created.
     */
    val timeNanos: Long
        get() = System.nanoTime() - startTime
}
