package me.anno.utils.structures

/**
 * a task is a runnable with a name, and approximate relative cost
 * (to estimate whether a task can still be completed within that respective frame, or whether it should be delayed)
 * */
class Task(val name: String, val cost: Int, val work: () -> Unit)