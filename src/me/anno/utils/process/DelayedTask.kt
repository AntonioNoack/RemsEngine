package me.anno.utils.process

import me.anno.engine.Events.addEvent

/**
 * this class represents a task, that should run only a few times, and not necessarily immediately,
 * like saving a code file while it is edited:
 * it still should be saved, but saving every 500ms for example is fine, instead of every letter
 * */
class DelayedTask(
    private val runTask: () -> Unit,
    private val delayMillis: Long = 500L
) {

    constructor(function: () -> Unit) : this(function, 500)

    var isWorking = false

    fun update() {
        synchronized(this) {
            if (isWorking) return
            isWorking = true
        }
        addEvent("DelayedTask", delayMillis) {
            runTask()
            isWorking = false
        }
    }
}