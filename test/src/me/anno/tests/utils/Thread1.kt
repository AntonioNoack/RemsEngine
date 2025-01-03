package me.anno.tests.utils

/**
 * simulates another thread without actually needing to wait actual time;
 * saves sleeping runtime overhead;
 *
 * cannot test synchronization issues
 * */
class Thread1 {

    private val tasks = ArrayList<() -> Unit>()
    operator fun plusAssign(task: () -> Unit) {
        tasks += task
    }

    fun work() {
        while (tasks.isNotEmpty()) {
            tasks.removeFirst()()
        }
    }
}