package me.anno.engine

open class NamedTask(val name: String, val runnable: () -> Unit)