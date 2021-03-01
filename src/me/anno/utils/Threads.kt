package me.anno.utils

import kotlin.concurrent.thread

object Threads {

    private class MutableInt(var value: Int = 0)

    private val names = HashMap<String, MutableInt>()

    fun threadWithName(name: String, run: () -> Unit): Thread {
        val id = synchronized(names) {
            names.getOrPut(name) { MutableInt() }.value++
        }
        val name2 = "$name-$id"
        return thread(block = run, name = name2)
    }

}