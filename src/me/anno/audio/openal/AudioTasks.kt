package me.anno.audio.openal

import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.async.Queues
import me.anno.utils.structures.Task
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

object AudioTasks {

    private val audioTasks: Queue<Task> = ConcurrentLinkedQueue()
    private val nextAudioTasks = ArrayList<Task>()

    fun addAudioTask(name: String, weight: Int, task: () -> Unit) {
        // could be optimized for release...
        audioTasks.add(Task(name, weight, task))
    }

    fun addNextAudioTask(name: String, weight: Int, task: () -> Unit) {
        // could be optimized for release...
        synchronized(nextAudioTasks) {
            nextAudioTasks.add(Task(name, weight, task))
        }
    }

    fun workQueue() {
        synchronized(nextAudioTasks) {
            if (nextAudioTasks.isNotEmpty()) {
                audioTasks.addAll(nextAudioTasks)
                nextAudioTasks.clear()
            }
        }
        Queues.workQueue(audioTasks, 16 * MILLIS_TO_NANOS, false)
    }
}