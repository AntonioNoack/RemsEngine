package me.anno.engine

open class WaitingTask(val name: String, val canBeKilled: Boolean, val condition: () -> Boolean, var runnable: (() -> Unit)?)