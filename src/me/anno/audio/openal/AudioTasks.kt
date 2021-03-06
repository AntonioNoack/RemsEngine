package me.anno.audio.openal

import me.anno.gpu.GFX.workQueue
import me.anno.gpu.Task
import java.util.concurrent.ConcurrentLinkedQueue

object AudioTasks {

    private val audioTasks = ConcurrentLinkedQueue<Task>()
    private val nextAudioTasks = ArrayList<Task>()

    fun addTask(name: String, weight: Int, task: () -> Unit) {
        // could be optimized for release...
        audioTasks.add(Task(name, weight, task))
    }

    fun addNextTask(name: String, weight: Int, task: () -> Unit) {
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
        workQueue(audioTasks, 1f / 60f, false)
    }


}