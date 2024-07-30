package me.anno.gpu.query

import me.anno.utils.assertions.assertTrue

class StackableQueryData(var target: Int) {

    // there can only be one query active at a time, so implement a hierarchy/stack,
    //  and we'll assume that they run sequentially, because most likely they'll do
    var currentTimer: SimpleGPUQuery? = null
    val currentlyActive = ArrayList<StackableGPUQuery>()

    fun stopLastTimer() {
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

    fun startTimer(timer: SimpleGPUQuery) {
        assertTrue(timer.start())
        currentTimer = timer
    }
}