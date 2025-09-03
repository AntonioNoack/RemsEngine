package me.anno.engine

open class WaitingTask(
    name: String, val canBeKilled: Boolean,
    val condition: () -> Boolean, runnable: () -> Unit
) : NamedTask(name, runnable) {
    var canBeRemoved = false
}