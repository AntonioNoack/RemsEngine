package me.anno.utils.process

import kotlin.concurrent.thread

class DelayedTask(
    private val function: () -> Unit,
    private val delayMillis: Long = 500
) {

    constructor(function: () -> Unit) : this(function, 500)

    var isSaving = false
    fun update() {
        synchronized(this) {
            if (isSaving) return
            isSaving = true
        }
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
                // we need to unlock it anyways
            }
            isSaving = false
        }
    }

}