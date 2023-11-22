package me.anno.io

import me.anno.Engine
import me.anno.utils.Sleep.sleepShortly
import java.io.InputStream
import java.util.concurrent.TimeoutException
import kotlin.math.abs

/**
 * an InputStream that times out after a defined delay;
 * only works on InputStreams, which implement the available() method
 * */
@Suppress("unused")
class TimeoutInputStream(
    private val input: InputStream,
    private val timeoutMillis: Long
) : InputStream() {

    private val timeoutNanos = timeoutMillis * 1_000_000

    override fun read(): Int {
        val startTime = System.nanoTime()
        while (input.available() < 1 && !Engine.shutdown) {
            sleepShortly(true)
            val time = System.nanoTime()
            if (abs(startTime - time) > timeoutNanos) {
                break
            }
        }
        if (input.available() < 1) {
            throw TimeoutException("Timeout of $timeoutMillis ms exceeded")
        }
        return input.read()
    }

    companion object {
        fun InputStream.withTimeout(millis: Long = 5000): InputStream {
            return if (this is TimeoutInputStream) this
            else TimeoutInputStream(this, millis)
        }
    }
}