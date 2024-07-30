package me.anno.gpu.query

import me.anno.cache.ICacheData
import me.anno.utils.assertions.assertTrue
import org.lwjgl.opengl.GL46C.GL_TIME_ELAPSED

class GPUClockNanos : QueryBase(), ICacheData {

    // there can only be one query active at a time, so implement a hierarchy/stack,
    //  and we'll assume that they run sequentially, because most likely they'll do
    companion object {
        var currentTimer: GPUQuery? = null
        val currentlyActive = ArrayList<GPUClockNanos>()
    }

    private val timer0 = GPUQuery(GL_TIME_ELAPSED)
    private val timer1 = GPUQuery(GL_TIME_ELAPSED)

    private fun stopLastTimer() {
        val lastTimer = currentTimer ?: return
        lastTimer.stop(true)
        currentTimer = null
        val dt = lastTimer.lastResult
        if (dt > 0) {
            // add time to all active instances
            for (i in currentlyActive.indices) {
                currentlyActive[i].currResult += dt
            }
        }
    }

    private fun startTimer(timer: GPUQuery) {
        assertTrue(timer.start())
        currentTimer = timer
    }

    fun start(): Boolean {
        stopLastTimer()
        currResult = 0L
        startTimer(timer0)
        currentlyActive.add(this)
        return true
    }

    fun stop() {
        stopLastTimer()
        lastResult = currResult
        currentlyActive.remove(this)
        if (currentlyActive.isNotEmpty()) {
            startTimer(timer1)
        }
    }

    private var currResult = 0L

    override fun destroy() {
        super.destroy()
        timer0.destroy()
        timer1.destroy()
    }
}