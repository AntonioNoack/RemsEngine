package me.anno.engine

import me.anno.utils.structures.Compare.ifSame

class ScheduledTask(name: String, val time: Long, runnable: () -> Unit) :
    NamedTask(name, runnable), Comparable<ScheduledTask> {

    override fun compareTo(other: ScheduledTask): Int {
        if (this === other) return 0
        return time.compareTo(other.time).ifSame(-1)
    }
}