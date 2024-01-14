package me.anno.utils.process

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.engine.Events.addEvent
import me.anno.utils.OS
import kotlin.concurrent.thread

class DelayedTask(
    private val function: () -> Unit,
    private val delayMillis: Long = 500
) {

    constructor(function: () -> Unit) : this(function, 500)

    var isWorking = false
    var endTime = 0L

    fun update() {
        synchronized(this) {
            if (isWorking) return
            isWorking = true
        }
        if (OS.isWeb) {// no threading supported rn
            endTime = Time.nanoTime + delayMillis * MILLIS_TO_NANOS
            webUpdate()
        } else {
            thread(name = "DelayedTask") {
                try {
                    Thread.sleep(delayMillis)
                } catch (e: InterruptedException) {
                    // mmh...
                }
                try {
                    function()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // something went wrong;
                    // we need to unlock it anyway
                }
                isWorking = false
            }
        }
    }

    private fun webUpdate() {
        if (Time.nanoTime >= endTime) {
            function()
            isWorking = false
        } else {
            addEvent(this::webUpdate)
        }
    }

}