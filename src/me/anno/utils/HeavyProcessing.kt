package me.anno.utils

object HeavyProcessing {
    private val queues = HashMap<String, ProcessingQueue>()
    fun addTask(queueGroup: String, task: () -> Unit) {
        val queue = queues.getOrPut(queueGroup) {
            val queue = ProcessingQueue(queueGroup)
            queue.start()
            queue
        }
        queue += task
    }
}