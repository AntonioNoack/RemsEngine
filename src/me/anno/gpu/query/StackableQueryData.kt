package me.anno.gpu.query

import me.anno.Time

class StackableQueryData(var target: Int) {

    // there can only be one query active at a time, so implement a hierarchy/stack,
    //  and we'll assume that they run sequentially, because most likely they'll do
    private var currentTimer: SimpleGPUQuery? = null
    private var startTimeCPU = 0L
    val currentlyActive = ArrayList<StackableGPUQuery>()

    fun stopLastTimer() {
        val lastTimer = currentTimer ?: return
        lastTimer.stop(true)
        currentTimer = null

        val dt = lastTimer.result
        val currTime = Time.nanoTime
        val dtCPU = currTime - startTimeCPU
        // add time to all active instances
        for (i in currentlyActive.indices) {
            val active = currentlyActive[i]
            active.currResultGPU += dt
            active.currTimeNanosCPU += dtCPU
        }
    }

    fun startTimer(timer: SimpleGPUQuery) {
        if (timer.start()) {
            currentTimer = timer
            startTimeCPU = Time.nanoTime
        }
    }
}